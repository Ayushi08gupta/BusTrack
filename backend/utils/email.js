const nodemailer = require('nodemailer');

const createTransporter = () => {
  return nodemailer.createTransport({
    service: 'gmail',
    auth: {
      user: process.env.EMAIL_USER,
      pass: process.env.EMAIL_PASS
    }
  });
};

const sendCredentialsEmail = async (email, name, username, tempPassword) => {
  try {
    const transporter = createTransporter();
    const mailOptions = {
      from: process.env.EMAIL_USER,
      to: email,
      subject: 'Welcome to BusTrack - Your Account Credentials',
      html: `
        <div style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
          <h2 style="color: #3F51B5;">Welcome, ${name}!</h2>
          <p>Your account has been created by the Administrator. Use the following credentials to log in:</p>
          <div style="background: #f4f4f4; padding: 15px; border-radius: 5px; border-left: 5px solid #3F51B5;">
            <p><strong>Username/Email:</strong> ${username}</p>
            <p><strong>Temporary Password:</strong> ${tempPassword}</p>
          </div>
          <p><strong>Note:</strong> You will be required to change your password upon your first login.</p>
          <p>Best regards,<br>BusTrack Team</p>
        </div>
      `
    };
    await transporter.sendMail(mailOptions);
    return true;
  } catch (error) {
    console.error('Email error:', error);
    return false;
  }
};

const sendResetPasswordEmail = async (email, token) => {
  try {
    const transporter = createTransporter();
    const resetUrl = `bustrack://reset-password?token=${token}`; // Deep link for Android
    const mailOptions = {
      from: process.env.EMAIL_USER,
      to: email,
      subject: 'BusTrack - Password Reset Request',
      html: `
        <div style="font-family: Arial, sans-serif; padding: 20px;">
          <h2>Password Reset Request</h2>
          <p>You requested a password reset. Please use the following token in the app or click the link:</p>
          <p><strong>Token:</strong> ${token}</p>
          <p>If you did not request this, please ignore this email.</p>
        </div>
      `
    };
    await transporter.sendMail(mailOptions);
    return true;
  } catch (error) {
    console.error('Email error:', error);
    return false;
  }
};

module.exports = { sendCredentialsEmail, sendResetPasswordEmail };
