package com.example.smartgrow.core;

import android.os.AsyncTask;
import android.util.Log;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class GMailSender {


    private static final String SENDER_EMAIL = "29ljdiodos@gmail.com";
    private static final String SENDER_PASSWORD = "docnhhjfclpbfyiz";

    public interface EmailListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void sendOTP(String recipientEmail, String otpCode, EmailListener listener) {
        String subject = "SmartGrow Account Verification Code";
        String body = "Hello,\n\nYour 4-digit verification code is: " + otpCode + "\n\nPlease use this to reset your password.\n\nThank you,\nSmartGrow Team";

        new SendEmailTask(recipientEmail, subject, body, listener).execute();
    }

    private static class SendEmailTask extends AsyncTask<Void, Void, Boolean> {
        private String recipient;
        private String subject;
        private String body;
        private EmailListener listener;
        private Exception exception;

        public SendEmailTask(String recipient, String subject, String body, EmailListener listener) {
            this.recipient = recipient;
            this.subject = subject;
            this.body = body;
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.socketFactory.port", "465");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.port", "465");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);
                return true;
            } catch (Exception e) {
                this.exception = e;
                Log.e("GMailSender", "Error sending email", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                if (listener != null) listener.onSuccess();
            } else {
                if (listener != null) listener.onFailure(exception);
            }
        }
    }
}