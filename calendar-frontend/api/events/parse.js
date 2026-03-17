const OpenAI = require('openai');

function extractField(response, fieldName) {
    // Match the field and capture only until end of that line
    const regex = new RegExp(`^${fieldName}:\\s*(.*)$`, 'm');
    const match = response.match(regex);
    if (!match) return null;
    const val = match[1].trim();
    // Return null if empty or placeholder text
    if (!val || val.toLowerCase() === 'empty' || val.toLowerCase() === 'none' || val === '-') return null;
    return val;
}

function addOneHour(timeStr) {
    if (!timeStr) return null;
    const [hours, minutes] = timeStr.split(':').map(Number);
    const date = new Date();
    date.setHours(hours, minutes, 0, 0);
    date.setHours(date.getHours() + 1);
    const h = String(date.getHours()).padStart(2, '0');
    const m = String(date.getMinutes()).padStart(2, '0');
    return `${h}:${m}`;
}

function isTimeFormat(val) {
    // Must match HH:MM format only
    return /^\d{2}:\d{2}$/.test(val);
}

function parseAIResponse(response) {
    const title = extractField(response, 'TITLE');
    const dateStr = extractField(response, 'DATE');
    const startTime = extractField(response, 'START_TIME');
    let endTime = extractField(response, 'END_TIME');
    const location = extractField(response, 'LOCATION');

    if (!title) return { successful: false, errorMessage: 'Could not extract event title' };
    if (!dateStr) return { successful: false, errorMessage: 'Could not extract event date' };

    // Validate end time — if it's not a proper HH:MM, discard it
    if (endTime && !isTimeFormat(endTime)) {
        endTime = null;
    }

    // Smart inference: if no end time, default to 1 hour after start
    if (!endTime && startTime && isTimeFormat(startTime)) {
        endTime = addOneHour(startTime);
    }

    return {
        successful: true,
        title,
        date: dateStr,
        startTime: startTime || '09:00',
        endTime: endTime,
        location: location || null
    };
}

module.exports = async function handler(req, res) {
    if (req.method !== 'POST') return res.status(405).end();

    const { text } = req.body;
    if (!text || !text.trim()) {
        return res.status(400).json({ successful: false, errorMessage: 'Please provide an event description' });
    }

    if (!process.env.GROQ_API_KEY) {
        return res.status(500).json({ successful: false, errorMessage: 'AI service not configured' });
    }

    try {
        const openai = new OpenAI({
            apiKey: process.env.GROQ_API_KEY,
            baseURL: 'https://api.groq.com/openai/v1',
        });

        const today = new Date().toISOString().split('T')[0];
        const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];

        const prompt = `You are a calendar assistant. Parse the following user input into calendar event details.
Today's date is: ${today}

User input: "${text}"

Respond ONLY in this exact format with each field on its own line. Do not add any extra text:
ACTION: CREATE
TITLE: [event title]
DATE: [YYYY-MM-DD]
START_TIME: [HH:MM in 24-hour format]
END_TIME: [HH:MM in 24-hour format, or leave completely blank]
LOCATION: [location, or leave completely blank]

Rules:
- END_TIME and LOCATION must be on their own lines, even if blank
- Convert relative dates like "tomorrow", "next Monday" to actual dates
- Convert 12-hour time (2 PM) to 24-hour format (14:00)
- If no end time is mentioned, leave END_TIME blank
- Keep title concise

Example output:
ACTION: CREATE
TITLE: Meeting with John
DATE: ${tomorrow}
START_TIME: 14:00
END_TIME:
LOCATION:`;

        const result = await openai.chat.completions.create({
            model: 'llama-3.3-70b-versatile',
            messages: [{ role: 'user', content: prompt }],
            max_tokens: 200,
            temperature: 0,
        });

        const aiText = result.choices[0].message.content;
        console.log('AI Response:', aiText); // helpful for debugging
        const parsed = parseAIResponse(aiText);

        return res.status(200).json(parsed);
    } catch (e) {
        console.error('Parse error:', e.message);
        return res.status(500).json({ successful: false, errorMessage: 'Failed to parse: ' + e.message });
    }
};