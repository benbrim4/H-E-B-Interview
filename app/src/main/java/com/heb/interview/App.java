/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.heb.interview;

import java.util.concurrent.TimeUnit;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.media.jsonb.JsonbSupport;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.media.multipart.MultiPartSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.staticcontent.StaticContentSupport;

public class App {

    public static void main(String[] args) {
        Config config = Config.create();
        WebServer webServer = WebServer.builder()
                .routing(createRouting(config))
                .config(config.get("server"))
                .addMediaSupport(JsonpSupport.create())
                .addMediaSupport(JsonbSupport.create())
                .addMediaSupport(MultiPartSupport.create())
                .build()
                .start()
                .await(10, TimeUnit.SECONDS);

        System.out.println("Server started at: http://localhost:" + webServer.port());
    }

    private static Routing createRouting(Config config) {
        Config dbConfig = config.get("db");

        DbClient dbClient = DbClient.builder(dbConfig)
                .build();

        return Routing.builder()
                .register("/images", new ImageService(dbClient))
                .any("/", (req, res) -> {
                    res.status(Http.Status.MOVED_PERMANENTLY_301);
                    res.headers().put(Http.Header.LOCATION, "/ui");
                    res.send();
                })
                .register("/ui", StaticContentSupport.builder("/WEB").welcomeFileName("index.html").build())
                .build();

    }

}
