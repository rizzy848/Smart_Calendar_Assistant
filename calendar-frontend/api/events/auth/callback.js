const { google } = require('googleapis');

module.exports = async function handler(req, res) {
    const { code } = req.query;

    if (!code) {
        return res.status(400).send('<h1>Error: No authorization code received</h1>');
    }

    try {
        const oauth2Client = new google.auth.OAuth2(
            process.env.GOOGLE_CLIENT_ID,
            process.env.GOOGLE_CLIENT_SECRET,
            process.env.OAUTH_REDIRECT_URI
        );

        const { tokens } = await oauth2Client.getToken(code);

        const html = `<!DOCTYPE html>
<html>
<head>
    <title>Authentication Successful</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }
        .container {
            background: white;
            padding: 40px;
            border-radius: 20px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            text-align: center;
            max-width: 400px;
        }
        .checkmark { font-size: 64px; color: #28a745; margin-bottom: 20px; }
        h1 { color: #28a745; }
        p { color: #666; }
    </style>
</head>
<body>
    <div class="container">
        <div class="checkmark">✓</div>
        <h1>Authentication Successful!</h1>
        <p>You can now close this window and return to the application.</p>
        <p style="font-size: 12px; color: #999;">This window will close automatically in 3 seconds...</p>
    </div>
    <script>
        if (window.opener) {
            window.opener.postMessage({ type: 'oauth-success', tokens: ${JSON.stringify(tokens)} }, '*');
        }
        setTimeout(() => window.close(), 3000);
    </script>
</body>
</html>`;

        return res.status(200).setHeader('Content-Type', 'text/html').send(html);
    } catch (e) {
        console.error('OAuth callback error:', e.message);
        return res.status(400).send(`
            <h1>Authentication Error</h1>
            <p>${e.message}</p>
            <button onclick="window.close()">Close Window</button>
        `);
    }
};
