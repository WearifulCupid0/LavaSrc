package com.github.topi314.lavasrc.applemusic;

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppleMusicTokenTracker {
    private static final Logger log = LoggerFactory.getLogger(AppleMusicTokenTracker.class);
    public static final Pattern TOKEN_SCRIPT_PATTERN = Pattern.compile("const \\w{2}=\"(?<token>(ey[\\w-]+)\\.([\\w-]+)\\.([\\w-]+))\"");
    private final HttpInterfaceManager httpInterfaceManager;

    private String token;
    private String origin;
    private Instant expires;

    public AppleMusicTokenTracker(HttpInterfaceManager httpInterfaceManager) {
        this.httpInterfaceManager = httpInterfaceManager;
    }

    public boolean needUpdate() {
        return token == null || this.expires == null || this.expires.isBefore(Instant.now());
    }

    public void updateToken() {
        if(!needUpdate()) {
            log.debug("AppleMusic access token was recently updated, not updating again right away.");
            return;
        }

        log.info("Updating AppleMusic access token (current is {}).", this.token);

        try {
            fetchToken();
            log.info("Updating AppleMusic access token succeeded, new token is {}.", this.token);
        } catch (Exception e) {
            log.error("AppleMusic access token update failed.", e);
        }
    }

    private void fetchToken() throws Exception {
        HttpGet get = new HttpGet("https://music.apple.com");
        try (CloseableHttpResponse response = httpInterfaceManager.getInterface().execute(get)) {
            HttpClientTools.assertSuccessWithContent(response, "apple music token response");

            Document document = Jsoup.parse(response.getEntity().getContent(), null, "");
            Elements elements = document.select("script[type=module][src~=/assets/index.*.js]");
            if (elements.isEmpty()) {
                throw new IllegalStateException("Cannot find token script element");
            }

            for (Element element : elements) {
                String tokenScriptURL = element.attr("src");
                get = new HttpGet("https://music.apple.com" + tokenScriptURL);
                try (CloseableHttpResponse indexResponse = httpInterfaceManager.getInterface().execute(get)) {
                    String tokenScript = IOUtils.toString(indexResponse.getEntity().getContent(), StandardCharsets.UTF_8);
                    Matcher tokenMatcher = TOKEN_SCRIPT_PATTERN.matcher(tokenScript);
                    if (tokenMatcher.find()) {
                        this.token = tokenMatcher.group("token");
                        if (this.token == null || this.token.isEmpty()) continue;
                        JsonBrowser json = JsonBrowser.parse(new String(Base64.getDecoder().decode(this.token.split("\\.")[1])));
                        this.expires = Instant.ofEpochSecond(json.get("exp").asLong(0));
                        this.origin = json.get("root_https_origin").index(0).text();
                        break;
                    }
                }
            }
        }
        if (this.token == null || this.token.isEmpty()) {
            throw new IllegalStateException("Cannot find token script url");
        }
    }

    public String getToken() {
        if (this.needUpdate()) this.updateToken();
        return "Bearer " + this.token;
    }

    public String getOrigin() {
        if (this.needUpdate()) this.updateToken();
        return this.origin;
    }
}
