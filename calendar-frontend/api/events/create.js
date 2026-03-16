const { google } = require('googleapis');

module.exports = async function handler(req, res) {
    if (req.method !== 'POST') return res.status(405).end();

    const { title, date, startTime, endTime, location } = req.body;
    const authHeader = req.headers['authorization'];

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ success: false, message: 'Not authenticated with Google Calendar' });
    }

    if (!title || !date || !startTime) {
        return res.status(400).json({ success: false, message: 'Title, date and start time are required' });
    }

    let tokens;
    try {
        const tokenB64 = authHeader.replace('Bearer ', '');
        tokens = JSON.parse(Buffer.from(tokenB64, 'base64').toString('utf-8'));
    } catch (e) {
        return res.status(401).json({ success: false, message: 'Invalid authorization token' });
    }

    try {
        const oauth2Client = new google.auth.OAuth2(
            process.env.GOOGLE_CLIENT_ID,
            process.env.GOOGLE_CLIENT_SECRET,
            process.env.OAUTH_REDIRECT_URI
        );
        oauth2Client.setCredentials(tokens);

        const calendar = google.calendar({ version: 'v3', auth: oauth2Client });

        const startDateTime = new Date(`${date}T${startTime}:00`);
        const endDateTime = endTime
            ? new Date(`${date}T${endTime}:00`)
            : new Date(startDateTime.getTime() + 3600000);

        const event = await calendar.events.insert({
            calendarId: 'primary',
            resource: {
                summary: title,
                location: location || undefined,
                start: { dateTime: startDateTime.toISOString() },
                end: { dateTime: endDateTime.toISOString() }
            }
        });

        return res.status(200).json({
            success: true,
            message: 'Event created successfully!',
            eventId: event.data.id,
            eventLink: event.data.htmlLink
        });
    } catch (e) {
        console.error('Create event error:', e.message);
        if (e.code === 401 || e.status === 401) {
            return res.status(401).json({ success: false, message: 'Google authentication expired. Please re-authenticate.' });
        }
        return res.status(500).json({ success: false, message: 'Failed to create event: ' + e.message });
    }
};
