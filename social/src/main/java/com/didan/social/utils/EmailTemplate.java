package com.didan.social.utils;

/**
 * Mẫu email HTML dùng chung (inline-style để hiển thị tốt trên Gmail/Outlook).
 */
public final class EmailTemplate {
    private EmailTemplate() {}

    private static final String BRAND = "HIPDN-EA Forum";
    private static final String ACCENT = "#2577b1";

    /** Email dạng mã OTP: tiêu đề + đoạn dẫn + ô mã lớn + ghi chú nhỏ. */
    public static String otp(String heading, String intro, String code, String note) {
        return wrap(
            "<h1 style=\"margin:0 0 12px;font-size:20px;color:#111827\">" + esc(heading) + "</h1>"
          + "<p style=\"margin:0 0 20px;font-size:14px;line-height:1.6;color:#374151\">" + esc(intro) + "</p>"
          + "<div style=\"margin:0 auto 20px;max-width:280px;background:#f3f4f6;border:1px solid #e5e7eb;"
          +   "border-radius:10px;padding:16px;text-align:center\">"
          +   "<div style=\"font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#6b7280;margin-bottom:6px\">Mã xác thực</div>"
          +   "<div style=\"font-family:'Courier New',monospace;font-size:30px;font-weight:700;letter-spacing:.35em;color:" + ACCENT + "\">"
          +     esc(code) + "</div>"
          + "</div>"
          + "<p style=\"margin:0;font-size:12px;line-height:1.6;color:#9ca3af\">" + esc(note) + "</p>"
        );
    }

    private static String wrap(String inner) {
        return "<!doctype html><html><body style=\"margin:0;padding:0;background:#f4f6f8\">"
          + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f6f8;padding:24px 0\">"
          + "<tr><td align=\"center\">"
          + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" "
          +   "style=\"width:480px;max-width:92%;background:#ffffff;border-radius:14px;overflow:hidden;"
          +   "box-shadow:0 1px 3px rgba(0,0,0,.08);font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif\">"
          + "<tr><td style=\"background:" + ACCENT + ";padding:18px 28px\">"
          +   "<span style=\"color:#fff;font-size:16px;font-weight:700;letter-spacing:.01em\">" + BRAND + "</span>"
          + "</td></tr>"
          + "<tr><td style=\"padding:28px\">" + inner + "</td></tr>"
          + "<tr><td style=\"padding:16px 28px;background:#fafafa;border-top:1px solid #eee\">"
          +   "<span style=\"font-size:11px;color:#9ca3af\">Email tự động từ " + BRAND + " — vui lòng không trả lời.</span>"
          + "</td></tr>"
          + "</table></td></tr></table></body></html>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
