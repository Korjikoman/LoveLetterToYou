package com.example.myproject.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendVerificationEmail(String email, String verificationToken){
        String subject = "Email Verification";
        String path = "req/signup/verify";
        String message = "Click the button below to verify your email address: ";

        sendEmail(email, verificationToken, subject, path, message);

    }

    public void sendResetPasswordEmail(String email, String resetToken){
        String subject = "Password Recover";
        String path = "req/signup/reset-password";
        String message = "Click the button below to reset your password: ";

        sendEmail(email, resetToken, subject, path, message);

    }

    public void sendEmail(String email, String token, String subject, String path, String message ){
        try{
            String actionUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path(path).queryParam("token", token).toUriString();

            String content = """
                    <div style="background-color: #ffffff;
                        padding: 40px;
                        border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                        max-width: 400px;
                        text-align: center;">
                    <h1 style="font-size: 24px; margin-bottom: 20px;">%s</h1>
                    <p style="font-size: 16px; line-height: 1.5; margin-bottom: 30px;">%s</p>
                    <a href="%s" class="button">PROCEED</a>
                    <p style="font-size: 16px; line-height: 1.5; margin-bottom: 30px;">%s</p>
                    <p style="font-size: 14px; margin-top: 20px;">Если вы не запрашивали сброс, просто игнорируйте это сообщение. Сообщение сгенерировано нейросетью, отвечать на него не надо, 1нач3 мы при1дем zа т0б01! </p>
                    
                    </div>
                    """.formatted(subject, message, actionUrl, actionUrl);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setTo(email);
            helper.setSubject(subject);
            helper.setFrom(from);
            helper.setText(content, true);
            mailSender.send(mimeMessage);
            System.out.println("EMAIL SUCSESSFULLLLLLY SENT !!!!!!!!!!!!!!!!!!!!!!!!");
        } catch (Exception e){
            System.err.println("Sending email gone wrong, here's why: --> "+  e.getMessage());
        }


    }

}
