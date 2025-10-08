// ======================
// Chat UI element hooks
// ======================
const chatBody = document.querySelector(".chat-body");
const messageInput = document.querySelector(".message-input");
const sendMessage = document.querySelector("#send-message");
const fileInput = document.querySelector("#file-input");
const fileUploadWrapper = document.querySelector(".file-upload-wrapper");
const fileCancelButton = fileUploadWrapper.querySelector("#file-cancel");
const chatbotToggler = document.querySelector("#chatbot-toggler");
const closeChatbot = document.querySelector("#close-chatbot");

// ==============
// API setup
// ==============
// IMPORTANT: For production, don't expose your key in client JS.
// Use a tiny backend proxy and keep the key server-side.
const API_KEY = "AIzaSyCQZaLPV5xXjs65vh2f8L6HlwHAn8ouSocY"; // <- replace with your Gemini (AI Studio) key

// Prefer a stronger model first; fall back to the lighter one if overloaded or unavailable.
let MODEL = "gemini-2.0-flash";               // primary
let FALLBACK_MODEL = "gemini-2.0-flash-lite"; // fallback (usually more available)

// Build URL from the current MODEL each request (so fallback updates take effect)
const buildApiUrl = () =>
    `https://generativelanguage.googleapis.com/v1/models/${MODEL}:generateContent?key=${API_KEY}`;

// Simple sleep
const delay = (ms) => new Promise((r) => setTimeout(r, ms));

// Generic fetch with exponential backoff + one-time model fallback on 429/503 overloads
async function fetchWithBackoff(options, { maxRetries = 3, baseDelay = 700 } = {}) {
    let attempt = 0;
    let switched = false;
    while (true) {
        const url = buildApiUrl();
        const res = await fetch(url, options);
        const text = await res.text();
        let data;
        try { data = JSON.parse(text); } catch { data = { raw: text }; }

        if (res.ok) return data;

        const status = res.status;
        const msg = data?.error?.message || `HTTP ${status}`;
        const overload = status === 429 || status === 503 || /overloaded/i.test(msg);

        // If model alias isn't found (404) or "not supported", try fallback immediately once.
        const notFound = status === 404 || /not found|not supported/i.test(msg);

        if ((overload || notFound) && !switched && MODEL !== FALLBACK_MODEL) {
            MODEL = FALLBACK_MODEL; // switch once
            switched = true;
            // small jitter before retry after switching model
            await delay(250);
            continue;
        }

        if (overload && attempt < maxRetries) {
            attempt += 1;
            const wait = baseDelay * Math.pow(2, attempt - 1) + Math.floor(Math.random() * 250);
            await delay(wait);
            continue;
        }

        const err = new Error(msg);
        err.status = status;
        err.payload = data;
        throw err;
    }
}

// ===============================
// Initialize user message and file
// ===============================
const userData = {
    message: null,
    file: {
        data: null,
        mime_type: null,
    },
};

// ================
// Store chat state
// ================
const chatHistory = [];
const initialInputHeight = messageInput.scrollHeight;

// =============================================
// Create message element with dynamic classes
// =============================================
const createMessageElement = (content, ...classes) => {
    const div = document.createElement("div");
    div.classList.add("message", ...classes);
    div.innerHTML = content;
    return div;
};

// ==================================
// Generate bot response using API
// ==================================
const generateBotResponse = async (incomingMessageDiv) => {
    const messageElement = incomingMessageDiv.querySelector(".message-text");

    // Add user message to chat history
    const parts = [{ text: userData.message || "" }];
    if (userData.file?.data && userData.file?.mime_type) {
        parts.push({ inline_data: { data: userData.file.data, mime_type: userData.file.mime_type } });
    }
    chatHistory.push({ role: "user", parts });

    // Keep only the most recent N messages to reduce request size (helps during load)
    const MAX_TURNS = 8; // user+model messages (tune as you like)
    const trimmedHistory = chatHistory.slice(-MAX_TURNS);

    const requestOptions = {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ contents: trimmedHistory }),
    };

    try {
        const data = await fetchWithBackoff(requestOptions, { maxRetries: 3, baseDelay: 700 });

        // Extract and display bot's response text (with safe fallbacks)
        const apiResponseText =
            data?.candidates?.[0]?.content?.parts?.[0]?.text?.replace(/\*\*(.*?)\*\*/g, "$1").trim() ??
            JSON.stringify(data, null, 2);

        messageElement.innerText = apiResponseText;

        // Add bot response to chat history
        chatHistory.push({ role: "model", parts: [{ text: apiResponseText }] });
    } catch (error) {
        console.error(error);
        if (error.status === 429 || error.status === 503 || /overloaded/i.test(error.message)) {
            messageElement.innerText =
                "The model is busy right now. I tried a few times automatically and also switched to a lighter model. Please try again in a moment.";
        } else if (error.status === 404 || /not found|not supported/i.test(error.message)) {
            messageElement.innerText =
                "This model alias isn’t available for your key. Try switching to gemini-2.0-flash-lite.";
        } else if (error.status === 400 && /API key not valid|API_KEY_INVALID/i.test(error.message)) {
            messageElement.innerText =
                "Your API key seems invalid for this endpoint. Make sure it’s an AI Studio Gemini key.";
        } else {
            messageElement.innerText = error.message || "Something went wrong calling the API.";
        }
        messageElement.style.color = "#ff0000";
    } finally {
        // Reset user's file data, remove thinking indicator, and scroll to bottom
        userData.file = { data: null, mime_type: null };
        incomingMessageDiv.classList.remove("thinking");
        chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });
    }
};

// ==================================
// Handle outgoing user messages
// ==================================
const handleOutgoingMessage = (e) => {
    e.preventDefault();

    userData.message = messageInput.value.trim();
    if (!userData.message && !userData.file?.data) return; // ignore empty sends

    messageInput.value = "";
    messageInput.dispatchEvent(new Event("input"));
    fileUploadWrapper.classList.remove("file-uploaded");

    // Create and display user message
    const messageContent = `
    <div class="message-text"></div>
    ${
        userData.file?.data
            ? `<img src="data:${userData.file.mime_type};base64,${userData.file.data}" class="attachment" />`
            : ""
    }`;

    const outgoingMessageDiv = createMessageElement(messageContent, "user-message");
    outgoingMessageDiv.querySelector(".message-text").innerText = userData.message || "(attachment)";
    chatBody.appendChild(outgoingMessageDiv);
    chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });

    // Simulate bot response with thinking indicator after a short delay
    setTimeout(() => {
        const botContent = `
      <svg class="bot-avatar" xmlns="http://www.w3.org/2000/svg" width="50" height="50" viewBox="0 0 1024 1024">
        <path d="M738.3 287.6H285.7c-59 0-106.8 47.8-106.8 106.8v303.1c0 59 47.8 106.8 106.8 106.8h81.5v111.1c0 .7.8 1.1 1.4.7l166.9-110.6 41.8-.8h117.4l43.6-.4c59 0 106.8-47.8 106.8-106.8V394.5c0-59-47.8-106.9-106.8-106.9zM351.7 448.2c0-29.5 23.9-53.5 53.5-53.5s53.5 23.9 53.5 53.5-23.9 53.5-53.5 53.5-53.5-23.9-53.5-53.5zm157.9 267.1c-67.8 0-123.8-47.5-132.3-109h264.6c-8.6 61.5-64.5 109-132.3 109zm110-213.7c-29.5 0-53.5-23.9-53.5-53.5s23.9-53.5 53.5-53.5 53.5 23.9 53.5 53.5-23.9 53.5-53.5 53.5zM867.2 644.5V453.1h26.5c19.4 0 35.1 15.7 35.1 35.1v121.1c0 19.4-15.7 35.1-35.1 35.1h-26.5zM95.2 609.4V488.2c0-19.4 15.7-35.1 35.1-35.1h26.5v191.3h-26.5c-19.4 0-35.1-15.7-35.1-35.1zM561.5 149.6c0 23.4-15.6 43.3-36.9 49.7v44.9h-30v-44.9c-21.4-6.5-36.9-26.3-36.9-49.7 0-28.6 23.3-51.9 51.9-51.9s51.9 23.3 51.9 51.9z"/>
      </svg>
      <div class="message-text">
        <div class="thinking-indicator">
          <div class="dot"></div>
          <div class="dot"></div>
          <div class="dot"></div>
        </div>
      </div>`;

        const incomingMessageDiv = createMessageElement(botContent, "bot-message", "thinking");
        chatBody.appendChild(incomingMessageDiv);
        chatBody.scrollTo({ top: chatBody.scrollHeight, behavior: "smooth" });
        generateBotResponse(incomingMessageDiv);
    }, 600);
};

// =====================================
// Adjust input field height dynamically
// =====================================
messageInput.addEventListener("input", () => {
    messageInput.style.height = `${initialInputHeight}px`;
    messageInput.style.height = `${messageInput.scrollHeight}px`;
    document.querySelector(".chat-form").style.borderRadius =
        messageInput.scrollHeight > initialInputHeight ? "15px" : "32px";
});

// ===========================================
// Handle Enter key press for sending messages
// ===========================================
messageInput.addEventListener("keydown", (e) => {
    const userMessage = e.target.value.trim();
    if (e.key === "Enter" && !e.shiftKey && (userMessage || userData.file?.data) && window.innerWidth > 768) {
        handleOutgoingMessage(e);
    }
});

// ===========================================
// Handle file input change + preview selected
// ===========================================
fileInput.addEventListener("change", () => {
    const file = fileInput.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
        fileInput.value = "";
        fileUploadWrapper.querySelector("img").src = e.target.result;
        fileUploadWrapper.classList.add("file-uploaded");

        const base64String = e.target.result.split(",")[1];
        userData.file = { data: base64String, mime_type: file.type };
    };
    reader.readAsDataURL(file);
});

// ==================
// Cancel file upload
// ==================
fileCancelButton.addEventListener("click", () => {
    userData.file = { data: null, mime_type: null };
    fileUploadWrapper.classList.remove("file-uploaded");
});

// ==========================================
// Initialize emoji picker and interactions
// ==========================================
const picker = new EmojiMart.Picker({
    theme: "light",
    skinTonePosition: "none",
    previewPosition: "none",
    onEmojiSelect: (emoji) => {
        const { selectionStart: start, selectionEnd: end } = messageInput;
        messageInput.setRangeText(emoji.native, start, end, "end");
        messageInput.focus();
    },
    onClickOutside: (e) => {
        if (e.target.id === "emoji-picker") {
            document.body.classList.toggle("show-emoji-picker");
        } else {
            document.body.classList.remove("show-emoji-picker");
        }
    },
});

document.querySelector(".chat-form").appendChild(picker);

// ===========================
// Button / UI event handlers
// ===========================
sendMessage.addEventListener("click", (e) => handleOutgoingMessage(e));
document.querySelector("#file-upload").addEventListener("click", () => fileInput.click());
closeChatbot.addEventListener("click", () => document.body.classList.remove("show-chatbot"));
chatbotToggler.addEventListener("click", () => document.body.classList.toggle("show-chatbot"));
