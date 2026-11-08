package com.railway.reservation.service.notify;

import org.springframework.web.util.HtmlUtils;

import java.util.List;

/** One plain, readable layout for every email the site sends: a heading, short paragraphs and a fact table. */
final class EmailLayout {

    private EmailLayout() {}

    static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }

    /** Label and value rows. Values are escaped here. */
    static String facts(List<String[]> rows) {
        StringBuilder sb = new StringBuilder("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;border-collapse:collapse;margin:0 0 20px\">");
        for (String[] r : rows) {
            sb.append("<tr><td style=\"padding:6px 12px 6px 0;font-size:13px;color:#4b5563;vertical-align:top;white-space:nowrap\">")
              .append(esc(r[0])).append("</td><td style=\"padding:6px 0;font-size:14px;font-weight:bold;vertical-align:top\">")
              .append(esc(r[1])).append("</td></tr>");
        }
        return sb.append("</table>").toString();
    }

    static String para(String text) {
        return "<p style=\"margin:0 0 14px;font-size:15px;line-height:1.5\">" + esc(text) + "</p>";
    }

    static String small(String text) {
        return "<p style=\"margin:0;font-size:13px;line-height:1.5;color:#4b5563\">" + esc(text) + "</p>";
    }

    static String page(String heading, String body) {
        return """
                <!doctype html>
                <html lang="en"><body style="margin:0;padding:24px;background:#f4f5f7;font-family:Arial,Helvetica,sans-serif;color:#111827">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"><tr><td align="center">
                <table role="presentation" width="560" cellpadding="0" cellspacing="0" style="max-width:560px;background:#ffffff;border:2px solid #111827;border-radius:8px">
                <tr><td style="padding:28px 32px">
                <p style="margin:0 0 20px;font-size:15px;font-weight:bold;letter-spacing:.02em">Railway Reservation</p>
                <h1 style="margin:0 0 16px;font-size:22px;line-height:1.25">%s</h1>
                %s
                </td></tr></table>
                </td></tr></table>
                </body></html>
                """.formatted(esc(heading), body);
    }
}
