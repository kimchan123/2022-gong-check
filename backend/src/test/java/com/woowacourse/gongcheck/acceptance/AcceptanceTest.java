package com.woowacourse.gongcheck.acceptance;

import static io.restassured.RestAssured.UNDEFINED_PORT;

import com.woowacourse.gongcheck.auth.application.EntranceCodeProvider;
import com.woowacourse.gongcheck.auth.application.response.GuestTokenResponse;
import com.woowacourse.gongcheck.auth.application.response.TokenResponse;
import com.woowacourse.gongcheck.auth.presentation.request.GuestEnterRequest;
import com.woowacourse.gongcheck.core.application.ImageUploader;
import com.woowacourse.gongcheck.core.application.NotificationService;
import com.woowacourse.gongcheck.core.domain.task.RunningTaskSseEmitterContainer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class AcceptanceTest {

    @MockBean
    private NotificationService notificationService;

    @MockBean
    protected RunningTaskSseEmitterContainer runningTaskSseEmitterContainer;

    @MockBean
    protected ImageUploader imageUploader;

    @Autowired
    private EntranceCodeProvider entranceCodeProvider;

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        if (RestAssured.port == UNDEFINED_PORT) {
            RestAssured.port = port;
        }
        databaseInitializer.initTable();
    }

    @AfterEach
    void clean() {
        databaseInitializer.truncateTables();
    }

    public String 토큰을_요청한다(final GuestEnterRequest guestEnterRequest) {
        String entranceCode = entranceCodeProvider.createEntranceCode(1L);
        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(guestEnterRequest)
                .when().post("/api/hosts/{entranceCode}/enter/", entranceCode)
                .then().log().all()
                .extract()
                .as(GuestTokenResponse.class)
                .getToken();
    }

    public TokenResponse Host_토큰을_요청한다() {
        return RestAssured
                .given().log().all()
                .when().post("/fake/login")
                .then().log().all()
                .extract()
                .as(TokenResponse.class);
    }
}
