const OpenAI = require('openai');

function extractField(response, fieldName) {
    const match = response.match(new RegExp(`${fieldName}:\\s*(.+)$`, 'm'));
    if (!match) return null;
    const val = match[1].trim();
    return val === '' || val.toLowerCase() === 'empty' ? null : val;
}

function parseAIResponse(response, originalInput) {
    const title = extractField(response, 'TITLE');
    const dateStr = extractField(response, 'DATE');
    const startTime = extractField(response, 'START_TIME');
    const endTime = extractField(response, 'END_TIME');
    const location = extractField(response, 'LOCATION');

    if (!title) return { successful: false, errorMessage: 'Could not extract event title' };
    if (!dateStr) return { successful: false, errorMessage: 'Could not extract event date' };

    return {
        successful: true,
        title,
        date: dateStr,
        startTime: startTime || '09:00',
        endTime: endTime || null,
        location: location || null
    };
}



module.exports = async function handler(req, res) {
    if (req.method !== 'POST') return res.status(405).end();

    const { text } = req.body;
    if (!text || !text.trim()) {
        return res.status(400).json({ successful: false, errorMessage: 'Please provide an event description' });
    }

    if (!process.env.OPENAI_API_KEY) {
        return res.status(500).json({ successful: false, errorMessage: 'AI service not configured' });
    }

    try {
        const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

        const today = new Date().toISOString().split('T')[0];
        const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];

        const prompt = `You are a calendar assistant. Parse the following user input into calendar event details.
Today's date is: ${today}

User input: "${text}"

Extract the following information and respond ONLY in this exact format:
ACTION: [CREATE/VIEW/DELETE/UPDATE]
TITLE: [event title]
DATE: [YYYY-MM-DD format]
START_TIME: [HH:MM in 24-hour format]
END_TIME: [HH:MM in 24-hour format, or leave empty]
LOCATION: [location or leave empty]

Rules:
- If no end time specified, leave END_TIME empty (default will be 1 hour after start)
- Convert relative dates like "tomorrow", "next Monday" to actual dates
- Convert 12-hour time (2 PM) to 24-hour format (14:00)
- If date is ambiguous, use the nearest future occurrence
- If action is unclear, default to CREATE
- Keep title concise but descriptive

Example:
Input: "Meeting with John tomorrow at 2 PM"
ACTION: CREATE
TITLE: Meeting with John
DATE: ${tomorrow}
START_TIME: 14:00
END_TIME:
LOCATION:

Now parse the user input above.`;

        const result = await openai.chat.completions.create({
            model: 'gpt-4o-mini',
            messages: [{ role: 'user', content: prompt }],
            max_tokens: 300,
        });

        const aiText = result.choices[0].message.content;
        const parsed = parseAIResponse(aiText, text);

        return res.status(200).json(parsed);
    } catch (e) {
        console.error('Parse error:', e.message);
        return res.status(500).json({ successful: false, errorMessage: 'Failed to parse: ' + e.message });
    }
};