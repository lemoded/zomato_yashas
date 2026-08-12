package com.company.framework.utils;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility for fetching OTP emails from Gmail via IMAP (SSL) using an App Password.
 */
public final class EmailUtils {

	private static final Logger log = LogManager.getLogger(EmailUtils.class);

	private EmailUtils() {
		throw new UnsupportedOperationException("EmailUtils is a static utility class");
	}

	/**
	 * Polls the Gmail inbox to fetch the latest Zomato OTP code.
	 *
	 * @param userEmail      Gmail address (e.g. contact.yashasy@gmail.com)
	 * @param appPassword    Gmail App Password (e.g. "meod tual erdd aiqo")
	 * @param timeoutSeconds max wait time in seconds
	 * @return 6-character OTP string or null if not found
	 */
	public static String fetchOtpFromEmail(String userEmail, String appPassword, int timeoutSeconds) {
		log.info("Polling Gmail inbox [{}] for Zomato OTP email (timeout={}s)...", userEmail, timeoutSeconds);
		long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

		Properties props = new Properties();
		props.put("mail.store.protocol", "imaps");
		props.put("mail.imaps.host", "imap.gmail.com");
		props.put("mail.imaps.port", "993");
		props.put("mail.imaps.ssl.enable", "true");
		props.put("mail.imaps.timeout", "5000");
		props.put("mail.imaps.connectiontimeout", "5000");

		String cleanPassword = appPassword.replaceAll("\\s+", "");

		while (System.currentTimeMillis() < endTime) {
			Store store = null;
			Folder inbox = null;
			try {
				Session session = Session.getInstance(props, null);
				store = session.getStore("imaps");
				store.connect("imap.gmail.com", userEmail, cleanPassword);

				inbox = store.getFolder("INBOX");
				inbox.open(Folder.READ_ONLY);

				Message[] messages = inbox.getMessages();
				int messageCount = messages.length;

				int start = Math.max(1, messageCount - 10);
				for (int i = messageCount; i >= start; i--) {
					Message msg = messages[i - 1];
					String subject = msg.getSubject();
					if (subject != null && subject.toLowerCase().contains("zomato")) {
						String body = getTextFromMessage(msg);
						String otp = extractOtp(body);
						if (otp != null) {
							log.info("Successfully fetched Zomato OTP from email: [{}]", otp);
							return otp;
						}
					}
				}
			} catch (Exception e) {
				log.warn("Polling email attempt failed: {}", e.getMessage());
			} finally {
				try {
					if (inbox != null && inbox.isOpen()) {
						inbox.close(false);
					}
					if (store != null && store.isConnected()) {
						store.close();
					}
				} catch (Exception ignored) {}
			}

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		log.error("Failed to fetch OTP from email within {} seconds", timeoutSeconds);
		return null;
	}

	private static String extractOtp(String text) {
		if (text == null || text.trim().isEmpty()) {
			return null;
		}

		// Strip HTML tags if present to extract clean visible text
		String plainText = text.replaceAll("<[^>]*>", " ").replaceAll("&nbsp;", " ");

		// 1. Primary Regex: Match 6-character OTP code immediately following "entering the following OTP"
		Matcher m1 = Pattern.compile("(?i)entering the following OTP\\s+([A-Z0-9]{6})\\b").matcher(plainText);
		if (m1.find()) {
			return m1.group(1).toUpperCase();
		}

		// 2. Secondary Regex: Match OTP code near keyword "OTP"
		Matcher m2 = Pattern.compile("(?i)\\bOTP\\b[\\s\\S]{1,50}?([A-Z0-9]{6})\\b").matcher(plainText);
		while (m2.find()) {
			String candidate = m2.group(1).toUpperCase();
			if (isStrictUppercaseOtp(candidate)) {
				return candidate;
			}
		}

		return null;
	}


	private static boolean isStrictUppercaseOtp(String str) {
		if (str == null || str.length() != 6) {
			return false;
		}
		// OTPs from Zomato are uppercase letters/digits and must contain at least one uppercase letter or digit
		if (!str.matches("[A-Z0-9]{6}")) {
			return false;
		}
		// Ignore common HTML/CSS/English 6-letter lowercase/mixed words that might be capitalized or parsed
		String upper = str.toUpperCase();
		return !upper.equals("BORDER") && !upper.equals("ZOMATO") && !upper.equals("EXPIRE") 
				&& !upper.equals("BUTTON") && !upper.equals("CENTER") && !upper.equals("SOLID")
				&& !upper.equals("MARGIN") && !upper.equals("HEIGHT");
	}


	private static String getTextFromMessage(Message message) throws Exception {
		if (message.isMimeType("text/plain")) {
			return message.getContent().toString();
		} else if (message.isMimeType("text/html")) {
			return message.getContent().toString();
		} else if (message.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) message.getContent();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < multipart.getCount(); i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				if (bodyPart.isMimeType("text/plain")) {
					sb.append(bodyPart.getContent());
				} else if (bodyPart.isMimeType("text/html")) {
					sb.append(bodyPart.getContent());
				}
			}
			return sb.toString();
		}
		return "";
	}
}
