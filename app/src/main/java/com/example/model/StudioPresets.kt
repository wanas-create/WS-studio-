package com.example.model

object StudioPresets {
    // 100+ Video Filters across 10 categories
    val filterCategories = listOf("Cinematic", "Vintage", "Cyberpunk", "Portrait", "Moody", "Vibrant", "B&W", "Teal & Orange", "Retro VHS", "Anime")

    val filters: List<Pair<String, String>> = buildList {
        // Cinematic
        addAll(listOf("Teal & Orange", "Blockbuster", "Hollywood Teal", "Director Cut", "Film Noir", "Anamorphic", "Dune Gold", "Matrix Green", "Gotham Night", "Interstellar").map { it to "Cinematic" })
        // Vintage
        addAll(listOf("1980 Kodachrome", "1970 Polaroid", "Super 8mm", "Old Tape 90s", "Sepia Warm", "Faded Nostalgia", "Vintage Paper", "Classic Bronze", "Warm 35mm", "Golden Hour 1960").map { it to "Vintage" })
        // Cyberpunk
        addAll(listOf("Neon Tokyo", "Cyber Violet", "Night City Cyan", "Laser Pink", "Blade Runner", "Synthetic Purple", "Electric Blue", "Matrix Code", "Arcade 1984", "Holographic").map { it to "Cyberpunk" })
        // Portrait
        addAll(listOf("Soft Glow Skin", "Porcelain Cream", "Sunset Glow", "Studio Softbox", "Peachy Warm", "Fresh Morning", "Golden Velvet", "Natural Beauty", "Highlight Pop", "Soft Glam").map { it to "Portrait" })
        // Moody
        addAll(listOf("Dark Cinema", "Nordic Winter", "Stormy Deep", "Midnight Forest", "Shadow Cast", "Cold Blue", "Dramatic Contrast", "Urban Concrete", "Subtle Fade", "Ghost Town").map { it to "Moody" })
        // Vibrant
        addAll(listOf("Hyper Saturation", "Tropical Paradise", "Summer Splash", "Candy Pop", "Ultra Vivid", "Sunburst", "Neon Cocktail", "Pop Art", "Color Punch", "Electric Meadow").map { it to "Vibrant" })
        // B&W
        addAll(listOf("High Contrast B&W", "Silver Metallic", "Silky Charcoal", "Monochrome 400", "Hard Shadow B&W", "Soft Matte Gray", "Street Snapshot", "Classic Leica", "Deep Obsidian", "Titanium Grey").map { it to "B&W" })
        // Teal & Orange
        addAll(listOf("T&O Moody", "T&O Miami", "T&O Sunset", "T&O Action", "T&O Pastel", "T&O Deep Shadows", "T&O Golden", "T&O Aqua", "T&O Vibrant", "T&O Clean").map { it to "Teal & Orange" })
        // Retro VHS
        addAll(listOf("Camcorder 1994", "VHS Static", "CRT TV Scan", "Betamax Tape", "Cassette Grain", "Magnetic Glitch", "Rewind 1988", "Hi8 Camera", "Analog Haze", "Tracking Error").map { it to "Retro VHS" })
        // Anime
        addAll(listOf("Makoto Sky", "Ghibli Green", "Tokyo Sunset", "Manga Cel", "Pastel Dream", "Sakura Pink", "Anime Glow", "Sparkle Fantasy", "Neon Shonen", "Watercolor Anime").map { it to "Anime" })
        // Extra studio additions to exceed 100
        for (i in 1..15) {
            add("Master LUT $i" to "Cinematic")
        }
    }

    // 100+ Video Effects
    val effectCategories = listOf("Glitch & VHS", "Light & Glow", "Motion & Shake", "Distortion & 3D", "Retro & Film", "Atmosphere & Sparks", "Cyber & Sci-Fi", "Party & Beat")

    val effects: List<Pair<String, String>> = buildList {
        // Glitch & VHS
        addAll(listOf("Glitch Shift", "RGB Split", "Pixel Sorter", "CRT Scanlines", "VHS Tape Tear", "Signal Drop", "Magnetic Distortion", "Glitch Flicker", "Digital Noise", "Bad Sync", "Data Moshing", "Chromance Tear").map { it to "Glitch & VHS" })
        // Light & Glow
        addAll(listOf("Edge Neon Glow", "Anamorphic Flare", "Sunlight Leak", "Soft Bloom", "Prism Rainbow", "Laser Beam", "Strobe Flash", "Bioluminescence", "Golden Aura", "Halo Ring", "Diamond Shine", "Lens Reflection").map { it to "Light & Glow" })
        // Motion & Shake
        addAll(listOf("Earthquake Shake", "Heartbeat Zoom", "Velocity Blur", "Whip Pan Left", "Speed Warp", "Camera Jitter", "Impact Bounce", "Dolly Zoom FX", "Dynamic Roll", "Micro Jitter", "Beat Pulse", "Turbulence").map { it to "Motion & Shake" })
        // Distortion & 3D
        addAll(listOf("3D Cube Twist", "Mirror Kaleidoscope", "Fish Eye Ultra", "Ripple Wave", "Swirl Vortex", "Time Freeze", "Tunnel Vision", "Water Droplet", "Sphere 3D", "Perspective Tilt", "Curved Screen", "Glass Refract").map { it to "Distortion & 3D" })
        // Retro & Film
        addAll(listOf("16mm Dust & Scratch", "Film Gate Weave", "Burn Edge", "Halation Red", "Grain Super 8", "Sprocket Holes", "Sepia Burn", "Silent Era Flicker", "Projector Light", "Vintage Vignette", "Kodak Halation", "Color Bleed").map { it to "Retro & Film" })
        // Atmosphere & Sparks
        addAll(listOf("Sparkle Particles", "Golden Embers", "Falling Rain", "Snow Flurry", "Cyber Fog", "Fireflies", "Smoke Plume", "Confetti Blast", "Laser Mist", "Light Orbs", "Magic Stardust", "Night Sparks").map { it to "Atmosphere & Sparks" })
        // Cyber & Sci-Fi
        addAll(listOf("Hologram Grid", "HUD Targeting", "Cyberpunk Scan", "Target Lock", "Thermal Vision", "Matrix Rain", "Binary Stream", "Night Vision", "Wireframe 3D", "Quantum Displace", "Shield Barrier", "Scanner Beam").map { it to "Cyber & Sci-Fi" })
        // Party & Beat
        addAll(listOf("Bass Strobe", "Neon Waveform", "Club Laser", "RGB Beat Flash", "Rainbow Strobing", "Equalizer Glow", "DJ Flash", "Psychedelic Swirl", "Disco Spotlight", "Pulse Wave", "Vibe Glitch", "Dynamic Rave").map { it to "Party & Beat" })
        // Additional studio effects to guarantee 100+
        for (i in 1..10) {
            add("VFX Master Preset $i" to "Cyber & Sci-Fi")
        }
    }

    // 50+ Transitions (Glitch, Blur, 3D, etc.)
    val transitions = listOf(
        "None",
        "Glitch Tear", "Glitch RGB Split", "Glitch Digital Block", "Glitch Static Burst", "Glitch Screen Flash",
        "Blur Zoom In", "Blur Zoom Out", "Directional Blur Right", "Radial Blur Spin", "Gaussian Cross-Fade",
        "3D Cube Flip", "3D Box Rotate", "3D Card Flip", "3D Door Open", "3D Page Turn", "3D Fold In",
        "Whip Pan Right", "Whip Pan Left", "Whip Pan Up", "Whip Pan Down",
        "Zoom Rush", "Super Spin", "Swirl Vortex", "Clockwise Roll", "Counter Roll",
        "Light Leak Gold", "Light Leak Prism", "Laser Flare Flash", "White Strobe", "Black Dip",
        "Luma Fade", "Dissolve Smooth", "Film Burn Transition", "Ink Splash", "Water Ripple",
        "Horizontal Slice", "Vertical Blinds", "Diagonal Wipe", "Circle Iris Open", "Star Iris",
        "Heart Wipe", "Split Screen In", "Push Left", "Slide Right", "Mosaic Shatter",
        "Paper Tear", "Retro Slide Roll", "Velocity Snap", "Mirror Swap", "Camera Shutter"
    )

    // 50+ Text Fonts
    val fonts = listOf(
        "Impact Pro", "Futuristic Heavy", "Cyberpunk Sans", "Bebas Bold", "Montserrat Ultra",
        "Cinematic Serif", "Neon Glow Script", "Street Graffiti", "Retro 80s Arcade", "Gothic Blackletter",
        "Modern Minimal", "Oswald Bold", "Playfair Elegant", "Roboto Slab", "Anton Display",
        "Courier Prime Code", "Permanent Marker", "Pacifico Brush", "Lobster Calligraphy", "Caveat Handwriting",
        "Great Vibes Classic", "Audiowide Tech", "Orbitron SciFi", "Press Start 2P", "Cinzel Decorative",
        "Abril Fatface", "Alfa Slab One", "Righteous Retro", "Comfortaa Round", "Fredoka Bubble",
        "Sacramento Cursive", "Shadows Into Light", "Yellowtail", "Bungee Outline", "Creepster Horror",
        "Monoton Vintage", "Faster One Speed", "Staatliches", "Poiret One Chic", "Syne Extra",
        "Cinematic Trajan", "Space Grotesk", "Clash Display", "Cabinet Grotesk", "Satoshi Bold",
        "General Sans", "DM Sans Ultra", "Inter Display", "Plus Jakarta Sans", "WS Signature Font"
    )

    // Text Animations
    val textAnimations = listOf(
        "Typewriter", "Wave Ripple", "Fade In", "Glitch Bounce", "Zoom Pop",
        "Slide Up", "Neon Flicker", "Spin In", "Letter Drop", "Elastic Expand", "Tracking Drift", "Shake Impact"
    )

    // Royalty Free Studio Beats
    val defaultMusicTracks = listOf(
        "Cyber Motion Bass (128 BPM)" to "WSS Studio",
        "Lo-Fi Sunset Chill (85 BPM)" to "ChillHop Beats",
        "Cinematic Epic Trailer" to "Hans W.",
        "Viral TikTok Velocity Trap" to "Metro Producer",
        "Summer Synthwave 1985" to "RetroWave Sound",
        "Deep House Club Punch" to "Studio EDM",
        "Acoustic Travel Vlog" to "Acoustic Vibe",
        "Heavy 808 Drill Bass" to "Urban Beats"
    )
}
