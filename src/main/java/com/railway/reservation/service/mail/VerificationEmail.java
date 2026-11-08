package com.railway.reservation.service.mail;

import org.springframework.web.util.HtmlUtils;

/** The sign-up code email, as plain text and as simple HTML. */
public final class VerificationEmail {

    private VerificationEmail() {}

    public static String subject(String code) {
        return code + " is your Railway Reservation code";
    }

    public static String text(String name, String code, int minutes) {
        return "Hello " + name + ",\n\n"
                + "Use this code to finish creating your Railway Reservation account:\n\n"
                + "    " + code + "\n\n"
                + "It expires in " + minutes + " minutes. Never share it with anyone.\n\n"
                + "If you did not try to create an account, you can ignore this email. "
                + "No account is created without the code.\n";
    }

    public static String html(String name, String code, int minutes) {
        String who = HtmlUtils.htmlEscape(name);
        return """
                <!doctype html>
                <html lang="en"><body style="margin:0;padding:24px;background:#f4f5f7;font-family:Arial,Helvetica,sans-serif;color:#111827">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"><tr><td align="center">
                <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="max-width:480px;background:#ffffff;border:2px solid #111827;border-radius:8px">
                <tr><td style="padding:28px 32px">
                <p style="margin:0 0 20px;font-size:15px;font-weight:bold;letter-spacing:.02em">Railway Reservation</p>
                <p style="margin:0 0 12px;font-size:15px">Hello %s,</p>
                <p style="margin:0 0 20px;font-size:15px;line-height:1.5">Use this code to finish creating your account:</p>
                <p style="margin:0 0 20px;font-size:32px;font-weight:bold;letter-spacing:.3em;font-family:'Courier New',monospace">%s</p>
                <p style="margin:0 0 12px;font-size:14px;line-height:1.5">It expires in %d minutes. Never share it with anyone.</p>
                <p style="margin:0;font-size:13px;line-height:1.5;color:#4b5563">If you did not try to create an account, you can ignore this email. No account is created without the code.</p>
                </td></tr></table>
                </td></tr></table>
                </body></html>
                """.formatted(who, code, minutes);
    }
}
