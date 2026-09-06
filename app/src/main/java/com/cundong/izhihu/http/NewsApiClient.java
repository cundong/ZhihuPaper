package com.cundong.izhihu.http;

import android.content.Context;

import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.entity.NewsListEntity;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Fetches and strictly validates the small JSON contract used by the reader. */
public final class NewsApiClient {

    private static final int MAX_JSON_BYTES = 6 * 1024 * 1024;
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final Gson GSON = new Gson();

    private final Context applicationContext;
    private final StopChecker stopChecker;
    public NewsApiClient(Context context, StopChecker stopChecker) {
        applicationContext = context.getApplicationContext();
        this.stopChecker = stopChecker;
    }

    public Payload<NewsListEntity> fetchNewsList(String url, String expectedDate)
            throws IOException {
        return parseNewsList(execute(url), expectedDate);
    }

    public Payload<NewsDetailEntity> fetchNewsDetail(String url, long expectedId)
            throws IOException {
        return parseNewsDetail(execute(url), expectedId);
    }

    public static Payload<NewsListEntity> parseNewsList(String rawJson, String expectedDate)
            throws InvalidPayloadException {
        JsonObject root = parseRootObject(rawJson);
        String date = requireString(root, "date");
        if (!date.matches("\\d{8}")) {
            throw new InvalidPayloadException("Invalid feed date");
        }
        if (!isBlank(expectedDate) && !expectedDate.equals(date)) {
            throw new InvalidPayloadException("Feed date does not match request");
        }

        JsonArray stories = requireArray(root, "stories");
        if (stories.size() == 0) {
            throw new InvalidPayloadException("Feed contains no stories");
        }
        for (JsonElement element : stories) {
            if (element == null || !element.isJsonObject()) {
                throw new InvalidPayloadException("Story is not an object");
            }
            JsonObject story = element.getAsJsonObject();
            long id = requirePositiveLong(story, "id");
            if (id <= 0) {
                throw new InvalidPayloadException("Invalid story id");
            }
            requireString(story, "title");
            validateOptionalWebUrl(story, "share_url");
            validateOptionalUrlArray(story, "images");
        }

        NewsListEntity entity;
        try {
            entity = GSON.fromJson(root, NewsListEntity.class);
        } catch (RuntimeException e) {
            throw new InvalidPayloadException("Unable to deserialize feed", e);
        }
        if (entity == null || entity.stories == null
                || entity.stories.size() != stories.size()) {
            throw new InvalidPayloadException("Incomplete feed model");
        }
        return new Payload<NewsListEntity>(rawJson, entity);
    }

    public static Payload<NewsDetailEntity> parseNewsDetail(String rawJson, long expectedId)
            throws InvalidPayloadException {
        JsonObject root = parseRootObject(rawJson);
        long id = requirePositiveLong(root, "id");
        if (expectedId <= 0 || id != expectedId) {
            throw new InvalidPayloadException("Article id does not match request");
        }
        requireString(root, "title");
        requireString(root, "body");
        validateOptionalWebUrl(root, "image");
        validateOptionalWebUrl(root, "share_url");
        validateOptionalUrlArray(root, "css");
        validateOptionalUrlArray(root, "js");

        NewsDetailEntity entity;
        try {
            entity = GSON.fromJson(root, NewsDetailEntity.class);
        } catch (RuntimeException e) {
            throw new InvalidPayloadException("Unable to deserialize article", e);
        }
        if (entity == null || entity.id != expectedId || isBlank(entity.body)) {
            throw new InvalidPayloadException("Incomplete article model");
        }
        return new Payload<NewsDetailEntity>(rawJson, entity);
    }

    private String execute(String url) throws IOException {
        throwIfStopped();
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build();
        Call call = HttpUtils.getInstance(applicationContext).newCall(request);
        Response response = null;
        try {
            response = call.execute();
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) {
                throw new IOException("Unexpected response code " + response.code());
            }
            long declaredLength = body.contentLength();
            if (declaredLength > MAX_JSON_BYTES) {
                throw new IOException("JSON response is too large");
            }
            return readLimited(body.byteStream(), call);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private String readLimited(InputStream input, Call call) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(16 * 1024);
        byte[] buffer = new byte[8 * 1024];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (isStopped()) {
                call.cancel();
                throw new InterruptedIOException("Offline sync was stopped");
            }
            total += read;
            if (total > MAX_JSON_BYTES) {
                call.cancel();
                throw new IOException("JSON response is too large");
            }
            output.write(buffer, 0, read);
        }
        throwIfStopped();
        return new String(output.toByteArray(), UTF_8);
    }

    private static JsonObject parseRootObject(String rawJson) throws InvalidPayloadException {
        if (isBlank(rawJson)) {
            throw new InvalidPayloadException("Empty JSON response");
        }
        try {
            JsonElement root = JsonParser.parseString(rawJson);
            if (root == null || !root.isJsonObject()) {
                throw new InvalidPayloadException("JSON root is not an object");
            }
            return root.getAsJsonObject();
        } catch (InvalidPayloadException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidPayloadException("Malformed JSON response", e);
        }
    }

    private static JsonArray requireArray(JsonObject object, String name)
            throws InvalidPayloadException {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonArray()) {
            throw new InvalidPayloadException("Missing array: " + name);
        }
        return value.getAsJsonArray();
    }

    private static String requireString(JsonObject object, String name)
            throws InvalidPayloadException {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive()) {
            throw new InvalidPayloadException("Missing string: " + name);
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (!primitive.isString()) {
            throw new InvalidPayloadException("Invalid string: " + name);
        }
        String result = primitive.getAsString();
        if (isBlank(result)) {
            throw new InvalidPayloadException("Blank string: " + name);
        }
        return result;
    }

    private static long requirePositiveLong(JsonObject object, String name)
            throws InvalidPayloadException {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new InvalidPayloadException("Missing number: " + name);
        }
        try {
            long result = value.getAsLong();
            if (result <= 0) {
                throw new InvalidPayloadException("Non-positive number: " + name);
            }
            return result;
        } catch (NumberFormatException e) {
            throw new InvalidPayloadException("Invalid number: " + name, e);
        }
    }

    private static void validateOptionalWebUrl(JsonObject object, String name)
            throws InvalidPayloadException {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            return;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || !isWebUrl(value.getAsString())) {
            throw new InvalidPayloadException("Invalid URL: " + name);
        }
    }

    private static void validateOptionalUrlArray(JsonObject object, String name)
            throws InvalidPayloadException {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) {
            return;
        }
        if (!value.isJsonArray()) {
            throw new InvalidPayloadException("Invalid URL array: " + name);
        }
        for (JsonElement element : value.getAsJsonArray()) {
            if (element == null || !element.isJsonPrimitive()
                    || !element.getAsJsonPrimitive().isString()
                    || !isWebUrl(element.getAsString())) {
                throw new InvalidPayloadException("Invalid URL in array: " + name);
            }
        }
    }

    private static boolean isWebUrl(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && !isBlank(uri.getHost()) && uri.getUserInfo() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private boolean isStopped() {
        return stopChecker != null && stopChecker.isStopped();
    }

    private void throwIfStopped() throws InterruptedIOException {
        if (isStopped()) {
            throw new InterruptedIOException("Offline sync was stopped");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    public interface StopChecker {
        boolean isStopped();
    }

    public static final class Payload<T> {
        public final String rawJson;
        public final T entity;

        Payload(String rawJson, T entity) {
            this.rawJson = rawJson;
            this.entity = entity;
        }
    }

    public static final class InvalidPayloadException extends IOException {
        InvalidPayloadException(String message) {
            super(message);
        }

        InvalidPayloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
