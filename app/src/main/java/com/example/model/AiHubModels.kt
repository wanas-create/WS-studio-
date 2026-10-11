package com.example.model

enum class AiCategory(val label: String, val icon: String) {
    ALL("All 50 AI", "✨"),
    TEXT("Text & Scripts", "💬"),
    IMAGE("Image & VFX", "🎨"),
    VIDEO("Video Generation", "🎬"),
    AUDIO("Voice & Beats", "🎵")
}

data class AiEngine(
    val id: String,
    val name: String,
    val category: AiCategory,
    val tag: String,
    val badgeColorHex: Long,
    val description: String,
    val samplePrompt: String
)

data class ChatMessage(
    val id: String,
    val aiEngineId: String,
    val aiName: String,
    val sender: String, // "user" or "ai"
    val text: String,
    val mediaType: String = "text", // "text", "image", "video", "audio"
    val mediaTitle: String = "",
    val mediaSubtitle: String = "",
    val mediaDuration: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object AiCatalog {
    val engines: List<AiEngine> = listOf(
        AiEngine("chatgpt", "ChatGPT", AiCategory.TEXT, "OpenAI GPT-4o", 0xFF10A37F, "Viral video scripts, scene hooks, and YouTube outlines.", "Write a 30s viral TikTok script about futuristic video editing."),
        AiEngine("gemini", "Gemini", AiCategory.TEXT, "Google Gemini 1.5", 0xFF1A73E8, "Multimodal creative director and SEO title generator.", "Generate 5 catchy YouTube Shorts titles and descriptions."),
        AiEngine("claude", "Claude", AiCategory.TEXT, "Anthropic 3.5 Sonnet", 0xFFD97706, "Deep cinematic storytelling and emotional voiceover scripts.", "Write a dramatic movie trailer voiceover for a sci-fi city."),
        AiEngine("perplexity", "Perplexity", AiCategory.TEXT, "Pro Search Engine", 0xFF0D9488, "Real-time trends, viral stats, and cited facts for documentaries.", "What are the top 3 trending editing styles on Instagram Reels right now?"),
        AiEngine("copilot", "Copilot", AiCategory.TEXT, "Microsoft AI", 0xFF2563EB, "Smart production assistant and storyboard checklist.", "Create a 5-step video production checklist for 4K YouTube uploads."),
        AiEngine("poe", "Poe", AiCategory.TEXT, "Multi-Bot Platform", 0xFF8B5CF6, "Fast access to multiple prompt engineers and creative bots.", "Give me 10 fast-paced transition sound effect ideas."),
        AiEngine("character_ai", "Character.AI", AiCategory.TEXT, "Persona Engine", 0xFFEC4899, "Roleplay dialogue and character voices for skits.", "Act as an intense Hollywood director giving feedback on my intro cut."),
        AiEngine("you_com", "You.com", AiCategory.TEXT, "Web Search Assistant", 0xFF06B6D4, "Rapid research and fact checking for commentary videos.", "Find viral sound trends used in fitness motivation edits."),

        // Image AI
        AiEngine("midjourney", "Midjourney", AiCategory.IMAGE, "v6.1 Photoreal", 0xFF6366F1, "Hyper-realistic 4K cinematics, cyber concept art, and thumbnails.", "Cinematic 4K still of neon Tokyo street in heavy rain, anamorphic lens --ar 16:9"),
        AiEngine("leonardo", "Leonardo", AiCategory.IMAGE, "Alchemic 3D", 0xFFF43F5E, "Gaming visuals, 3D character avatars, and fantasy backgrounds.", "3D futuristic mecha cockpit, glowing UI dashboard, 8k render"),
        AiEngine("ideogram", "Ideogram", AiCategory.IMAGE, "Typography Master", 0xFFE11D48, "Perfect text rendering in logos, posters, and YouTube titles.", "Poster with bold glowing typography reading 'WS STUDIO 4K'"),
        AiEngine("dall_e", "DALL-E", AiCategory.IMAGE, "OpenAI Image 3", 0xFF14B8A6, "Creative concept art and imaginative video backdrop assets.", "Surreal floating island with waterfalls into space, digital art"),
        AiEngine("firefly", "Firefly", AiCategory.IMAGE, "Adobe AI Engine", 0xFFF97316, "Commercial safe graphic textures, gradients, and overlay art.", "Abstract glass refraction background with cyan and royal blue lighting"),
        AiEngine("canva_ai", "Canva AI", AiCategory.IMAGE, "Magic Design", 0xFF06B6D4, "Instant social media thumbnail banners and stickers.", "YouTube thumbnail layout with 'SECRET EDITING TRICK' badge"),
        AiEngine("remove_bg", "Remove.bg", AiCategory.IMAGE, "Instant Cutout", 0xFF10B981, "1-tap transparent PNG cutout for video stickers and PIP.", "Remove background from subject portrait cleanly"),
        AiEngine("upscale", "Upscale", AiCategory.IMAGE, "4K Super-Res", 0xFF3B82F6, "Enhance low-res photos to crystal crisp 4K Ultra HD.", "Upscale low-res screenshot to 3840x2160 sharp detail"),

        // Video AI
        AiEngine("runway", "Runway", AiCategory.VIDEO, "Gen-3 Alpha", 0xFF8B5CF6, "Cinematic video generation with camera motion and physics.", "FPV drone rushing through neon cyber skyscrapers at night"),
        AiEngine("pika", "Pika", AiCategory.VIDEO, "Pika 2.0 Motion", 0xFFEC4899, "Smooth animation, lip sync, and dynamic camera moves.", "Anime warrior powering up with electric sparks and speed lines"),
        AiEngine("heygen", "HeyGen", AiCategory.VIDEO, "Avatar Pro 4K", 0xFF3B82F6, "Ultra-realistic talking head AI presenters in 40+ languages.", "Professional studio host introducing WS Studio features"),
        AiEngine("synthesia", "Synthesia", AiCategory.VIDEO, "Corporate Avatar", 0xFF059669, "Business training presenters with crisp natural speech.", "Corporate spokesperson giving a tutorial on timeline editing"),
        AiEngine("invideo", "InVideo", AiCategory.VIDEO, "AI Auto Editor", 0xFFF59E0B, "Full video compilation from script with b-rolls and subtitles.", "Create a 60-second summary video about Top 5 AI Video Trends"),
        AiEngine("pictory", "Pictory", AiCategory.VIDEO, "Long to Short", 0xFF6366F1, "Auto-extract viral highlights and vertical shorts from long videos.", "Convert podcast transcript into viral 30-second shorts"),
        AiEngine("luma", "Luma", AiCategory.VIDEO, "Dream Machine", 0xFFF43F5E, "Dreamlike camera sweeping shots and 3D environment flythroughs.", "Orbiting camera around an ancient crystal temple floating in clouds"),
        AiEngine("sora", "Sora", AiCategory.VIDEO, "OpenAI World Sim", 0xFF10A37F, "Unmatched physics and photorealistic 60FPS video clips.", "Woolly mammoth walking through snowy mountain valley at sunset"),

        // Audio AI
        AiEngine("elevenlabs", "ElevenLabs", AiCategory.AUDIO, "Prime Voice 4K", 0xFF2563EB, "Human-like expressive voiceovers, emotional tone, and accents.", "Narrate intro in deep movie-trailer voice: 'In a world of infinite creativity...'"),
        AiEngine("suno", "Suno", AiCategory.AUDIO, "V3.5 Song Craft", 0xFFEF4444, "Full original songs with vocals, lyrics, and modern beats.", "Create an energetic EDM phonk track with heavy 808 bass for gym edit"),
        AiEngine("udio", "Udio", AiCategory.AUDIO, "Clarity Master", 0xFF8B5CF6, "Studio quality multi-genre instrumentals and pristine audio.", "Compose an 80s synthwave retro beat with analog synthesizers"),
        AiEngine("murf", "Murf", AiCategory.AUDIO, "Studio Narration", 0xFF10B981, "Clean corporate and documentary voiceover tracks.", "Natural friendly narration for app onboarding guide"),
        AiEngine("whisper", "Whisper", AiCategory.AUDIO, "Auto Subtitles", 0xFF0D9488, "99.8% accurate auto transcription and viral karaoke captions.", "Transcribe speech and generate timed karaoke subtitle track"),
        AiEngine("eleven_reader", "ElevenReader", AiCategory.AUDIO, "Story Narrator", 0xFFD97706, "Audiobook and dramatic storytelling voices.", "Read story excerpt with warm British narrator accent"),

        // Text & Writing Tools
        AiEngine("grammarly", "Grammarly", AiCategory.TEXT, "Pro Tone Editor", 0xFF059669, "Refine video subtitles and script delivery for maximum punch.", "Rewrite my voiceover text to be more punchy and viral"),
        AiEngine("quillbot", "QuillBot", AiCategory.TEXT, "Paraphraser", 0xFF16A34A, "Reword captions and hooks to pass originality checks.", "Paraphrase script intro into 3 different catchy variations"),
        AiEngine("huggingface", "HuggingFace", AiCategory.TEXT, "Open Source Hub", 0xFFFFD21E, "Access thousands of community fine-tuned models.", "Find optimal open-source prompt for background removal"),
        AiEngine("grok", "Grok", AiCategory.TEXT, "xAI Real-time", 0xFF475569, "Witty, bold social commentary and real-time news hooks.", "Give me a witty sarcastic hot take on AI video tools"),
        AiEngine("mistral", "Mistral", AiCategory.TEXT, "Le Chat Fast", 0xFFFF7000, "Lightning fast structured JSON scripts and timing cues.", "Output 4 scene timestamps with recommended camera angles"),
        AiEngine("deepseek", "DeepSeek", AiCategory.TEXT, "V3 Deep Logic", 0xFF1E40AF, "Complex technical breakdown, math, and video compression advice.", "What is the optimal bitrate and CRF for 4K 60FPS H.265 export?"),
        AiEngine("claude_opus", "Claude Opus", AiCategory.TEXT, "Longform Cinema", 0xFFB45309, "Full screenplay writing and detailed character development.", "Write scene 1 screenplay for a cyberpunk detective short film"),

        // Advanced Image & VFX
        AiEngine("stable_diffusion", "Stable Diffusion", AiCategory.IMAGE, "SDXL Turbo", 0xFF475569, "Open source generation with ControlNet and custom LoRAs.", "Raw photo portrait, 35mm film grain, rim light, cinematic"),
        AiEngine("photoroom", "Photoroom", AiCategory.IMAGE, "Studio Studio Pro", 0xFF8B5CF6, "Professional product background replacements and shadows.", "Place product on dark reflective mirror surface with neon rim lighting"),
        AiEngine("flux", "Flux.1", AiCategory.IMAGE, "Black Forest Labs", 0xFF000000, "Next-generation realism, anatomy, and complex prompt adherence.", "Macro shot of human eye reflecting high-tech glowing holographic map"),
        AiEngine("krea_ai", "Krea AI", AiCategory.IMAGE, "Realtime Canvas", 0xFF06B6D4, "Live generative brush canvas and video enhancement.", "Generate smooth morphing abstract liquid metal backdrop"),
        AiEngine("magnific", "Magnific AI", AiCategory.IMAGE, "Hyper Detail", 0xFFBE185D, "Add hallucinated micro-details and 8K skin textures.", "Add cinematic film grain and ultra skin pores detail to close-up"),

        // Advanced Video & Audio
        AiEngine("descript", "Descript", AiCategory.VIDEO, "Text-to-Edit", 0xFF2563EB, "Edit video simply by editing the text transcript.", "Auto-cut filler words 'um' and 'uh' from voice recording"),
        AiEngine("kaiber", "Kaiber", AiCategory.VIDEO, "Audio Reactive", 0xFF7C3AED, "Music video visuals that pulse to the beat of your song.", "Audio-reactive anime transformation pulsing on drum beat"),
        AiEngine("suno_bark", "Suno Bark", AiCategory.AUDIO, "Sound FX Gen", 0xFFDC2626, "Generate custom sound effects: whooshes, risers, and explosions.", "Generate a deep cinema sub-bass drop and glitch riser"),
        AiEngine("viggle", "Viggle", AiCategory.VIDEO, "Motion Retarget", 0xFF059669, "Make any character dance or perform custom motion clips.", "Animate character doing viral hip-hop dance routine"),
        AiEngine("kling_ai", "Kling AI", AiCategory.VIDEO, "Dynamic 4K", 0xFFD97706, "10-second high frame rate realistic human movement clips.", "Subject doing backflip in slow motion with water splash"),
        AiEngine("hedra", "Hedra", AiCategory.VIDEO, "Expressive Faces", 0xFF9333EA, "Expressive vocal performances and character singing.", "Character singing upbeat pop song with natural head tilt"),
        AiEngine("topaz_ai", "Topaz AI", AiCategory.VIDEO, "FPS Enhancer", 0xFF0284C7, "Smooth 24FPS clips into fluid 60FPS or 120FPS slow motion.", "Interpolate 30fps clip to silky smooth 60fps slow-motion"),
        AiEngine("capcut_ai", "CapCut AI", AiCategory.VIDEO, "Smart Montage", 0xFF10B981, "Auto beat sync, velocity curves, and trending viral cuts.", "Auto-match cut transitions to every bass drop in the music")
    )

    fun getInitialMessages(): List<ChatMessage> {
        return emptyList()
    }

    fun generateReply(engine: AiEngine, userQuery: String): ChatMessage {
        val id = "reply_${System.currentTimeMillis()}"
        return when (engine.category) {
            AiCategory.TEXT -> {
                ChatMessage(
                    id = id,
                    aiEngineId = engine.id,
                    aiName = engine.name,
                    sender = "ai",
                    text = "🎬 **${engine.name} Video Production Strategy**\n\n" +
                            "**Viral Hook (0-3s):**\n" +
                            "\"Stop editing videos the slow way in 2026—here is the secret 4K workflow!\"\n\n" +
                            "**Visual Scene Direction (3-15s):**\n" +
                            "• Scene 1: Fast whip pan transition with RGB split.\n" +
                            "• Scene 2: 120FPS slow-mo speed ramp with bass drop.\n" +
                            "• Scene 3: Bold typography with neon glow font.\n\n" +
                            "**Call to Action (15-20s):**\n" +
                            "\"Exported in pristine 4K 60FPS with zero watermark in WS Studio. Tap link in bio!\"\n\n" +
                            "🔥 *Suggested Hashtags:* #VideoEditing #CapCutPro #WSStudio #4KEdit #ContentCreator"
                )
            }
            AiCategory.IMAGE -> {
                ChatMessage(
                    id = id,
                    aiEngineId = engine.id,
                    aiName = engine.name,
                    sender = "ai",
                    text = "Generated 4K UHD Concept Art for: \"$userQuery\"",
                    mediaType = "image",
                    mediaTitle = "${engine.name} 4K Asset Render",
                    mediaSubtitle = "Resolution 3840x2160 • Color Grade: Rec.709 • Free for Commercial Use"
                )
            }
            AiCategory.VIDEO -> {
                ChatMessage(
                    id = id,
                    aiEngineId = engine.id,
                    aiName = engine.name,
                    sender = "ai",
                    text = "Rendered high-dynamic video sequence with physics & cinematic camera motion:",
                    mediaType = "video",
                    mediaTitle = "${engine.name} Motion Clip",
                    mediaSubtitle = "60 FPS • 4K H.265 • Ready to Add to Timeline",
                    mediaDuration = "0:06"
                )
            }
            AiCategory.AUDIO -> {
                ChatMessage(
                    id = id,
                    aiEngineId = engine.id,
                    aiName = engine.name,
                    sender = "ai",
                    text = "Rendered studio-grade audio track for your project:",
                    mediaType = "audio",
                    mediaTitle = "${engine.name} Studio Audio",
                    mediaSubtitle = "Lossless WAV • 48kHz • 128 BPM Beat Synced",
                    mediaDuration = "0:28"
                )
            }
            AiCategory.ALL -> {
                ChatMessage(
                    id = id,
                    aiEngineId = engine.id,
                    aiName = engine.name,
                    sender = "ai",
                    text = "Processed query with ${engine.name}. All 50 models are synced with WS Studio timeline."
                )
            }
        }
    }
}
