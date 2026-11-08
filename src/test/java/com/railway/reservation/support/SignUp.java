package com.railway.reservation.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The two sign-up steps over HTTP: send details (a code is emailed), then confirm the code. */
public final class SignUp {

    private static final ObjectMapper JSON = new ObjectMapper();

    private SignUp() {}

    public static ResultActions start(MockMvc mvc, Map<String, ?> details) throws Exception {
        return mvc.perform(post("/api/v1/users/register").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(details)));
    }

    public static ResultActions verify(MockMvc mvc, String email, String code) throws Exception {
        return mvc.perform(post("/api/v1/users/register/verify").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("email", email, "code", code))));
    }

    /** Runs both steps and returns the body of the account-created response. */
    public static String complete(MockMvc mvc, MailCatcherConfig.MailCatcher mail, Map<String, ?> details) throws Exception {
        start(mvc, details).andExpect(status().isAccepted());
        String email = String.valueOf(details.get("email"));
        return verify(mvc, email, mail.codeFor(email)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
