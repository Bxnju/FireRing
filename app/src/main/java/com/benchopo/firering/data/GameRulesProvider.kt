package com.benchopo.firering.data

import com.benchopo.firering.model.*

object GameRulesProvider {

    fun getDefaultJackRules(gameMode: GameMode = GameMode.NORMAL): List<JackRule> {
        // Normal mode rules (base rules)
        val defaultRules = listOf(
            JackRule(
                id = "never_have_i_ever",
                title = "Never Have I Ever",
                description = "Say something you've never done. Everyone who has done it takes a drink.",
                type = RuleType.STANDARD,
                gameMode = GameMode.NORMAL
            ),
            JackRule(
                id = "truth_or_dare",
                title = "Truth or Dare",
                description = "Choose someone to answer a truth question or perform a dare.",
                type = RuleType.PHYSICAL,
                gameMode = GameMode.NORMAL
            ),
            JackRule(
                id = "most_likely_to",
                title = "Most Likely To",
                description = "Say something someone might do. Everyone points at who they think is most likely to do it. Person with most fingers pointing at them drinks.",
                type = RuleType.STANDARD,
                gameMode = GameMode.NORMAL
            ),
            JackRule(
                id = "would_you_rather",
                title = "Would You Rather",
                description = "Ask 'would you rather' questions. Everyone must answer or drink.",
                type = RuleType.STANDARD,
                gameMode = GameMode.NORMAL
            )
        )

        // Add game mode specific rules
        val modeRules = when(gameMode) {
            GameMode.CAMIONERO -> listOf(
                JackRule(
                    id = "road_stories",
                    title = "Road Stories",
                    description = "Share your craziest road trip story or drink THREE times.",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.CAMIONERO
                ),
                JackRule(
                    id = "truckers_challenge",
                    title = "Trucker's Challenge",
                    description = "Do 5 push-ups or drink FOUR times. Real truckers have strength!",
                    type = RuleType.PHYSICAL,
                    gameMode = GameMode.CAMIONERO
                ),
                JackRule(
                    id = "long_haul",
                    title = "Long Haul",
                    description = "Hold your breath for 30 seconds or drink twice. Long haul drivers need good lungs!",
                    type = RuleType.PHYSICAL,
                    gameMode = GameMode.CAMIONERO
                ),
                JackRule(
                    id = "cb_radio",
                    title = "CB Radio",
                    description = "Everyone must talk like a trucker using CB radio slang until your next turn or drink each time they mess up.",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.CAMIONERO
                ),
                JackRule(
                    id = "heavy_load",
                    title = "Heavy Load",
                    description = "The player with the most drinks must give out 3 more drinks to others. That's a heavy load to carry!",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.CAMIONERO
                )
            )

            GameMode.DESPECHADO -> listOf(
                JackRule(
                    id = "ex_confessions",
                    title = "Ex Confessions",
                    description = "Share something about your ex or drink three times. Let the pain out!",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.DESPECHADO
                ),
                JackRule(
                    id = "sad_song",
                    title = "Sad Song",
                    description = "Name a breakup song or drink twice. Bonus: sing a line and everyone drinks with you in solidarity.",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.DESPECHADO
                ),
                JackRule(
                    id = "love_scars",
                    title = "Love Scars",
                    description = "Show a photo or memento from a past relationship or drink. Let the wounds heal!",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.DESPECHADO
                ),
                JackRule(
                    id = "rebound",
                    title = "Rebound",
                    description = "Choose someone to be your 'rebound' who drinks whenever you do until your next turn.",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.DESPECHADO
                ),
                JackRule(
                    id = "emotional_damage",
                    title = "Emotional Damage",
                    description = "Everyone shares their worst heartbreak or drinks 3 times. Group therapy session begins now!",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.DESPECHADO
                )
            )

            GameMode.CALENTURIENTOS -> listOf(
                JackRule(
                    id = "spicy_truth",
                    title = "Spicy Truth",
                    description = "Answer a spicy question truthfully or drink three times.",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.CALENTURIENTOS
                ),
                JackRule(
                    id = "hot_dare",
                    title = "Hot Dare",
                    description = "Perform a flirty dare chosen by the group or drink twice.",
                    type = RuleType.PHYSICAL,
                    gameMode = GameMode.CALENTURIENTOS
                ),
                JackRule(
                    id = "secret_crush",
                    title = "Secret Crush",
                    description = "Name your celebrity crush or someone in the room you find attractive, or drink.",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.CALENTURIENTOS
                ),
                JackRule(
                    id = "body_heat",
                    title = "Body Heat",
                    description = "The person to your right must sit close to you until your next turn or both drink.",
                    type = RuleType.PHYSICAL,
                    gameMode = GameMode.CALENTURIENTOS
                ),
                JackRule(
                    id = "temptation",
                    title = "Temptation",
                    description = "Everyone anonymously votes who they find most attractive in the room, that person gives out 3 drinks.",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.CALENTURIENTOS
                )
            )

            GameMode.MEDIA_COPAS -> listOf(
                JackRule(
                    id = "light_sip",
                    title = "Light Sip",
                    description = "Everyone takes just a small sip of their drink. Take it easy!",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.MEDIA_COPAS
                ),
                JackRule(
                    id = "hydration_check",
                    title = "Hydration Check",
                    description = "Everyone must have a sip of water. Stay hydrated!",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.MEDIA_COPAS
                ),
                JackRule(
                    id = "gentle_spin",
                    title = "Gentle Spin",
                    description = "Spin in a circle once or take a tiny sip. No need to get dizzy!",
                    type = RuleType.PHYSICAL,
                    gameMode = GameMode.MEDIA_COPAS
                ),
                JackRule(
                    id = "break_time",
                    title = "Break Time",
                    description = "You can skip your next drinking punishment. Everyone needs a break sometimes!",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.MEDIA_COPAS
                ),
                JackRule(
                    id = "pace_yourself",
                    title = "Pace Yourself",
                    description = "Set a 2-drink limit for everyone until your next turn. Slow and steady!",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.MEDIA_COPAS
                )
            )

            else -> emptyList()
        }

        return defaultRules + modeRules
    }

    fun getDefaultMiniGames(gameMode: GameMode = GameMode.NORMAL): List<MiniGame> {
        // Normal mode mini games (base games)
        val defaultGames = listOf(
            MiniGame(
                id = "categories",
                title = "Categories",
                description = "Choose a category (e.g., car brands). Players take turns naming items in that category. First to repeat or fail drinks.",
                type = MiniGameType.CHALLENGE,
                gameMode = GameMode.NORMAL
            ),
            MiniGame(
                id = "thumb_master",
                title = "Thumb Master",
                description = "You're the Thumb Master. At any point, you can put your thumb on the table. Last person to notice and put their thumb down drinks.",
                type = MiniGameType.REACTION,
                gameMode = GameMode.NORMAL
            ),
            MiniGame(
                id = "counting_game",
                title = "Counting Game",
                description = "Count around the circle, but replace any number containing 7 or divisible by 7 with 'Buzz'. Failure means drink.",
                type = MiniGameType.CHALLENGE,
                gameMode = GameMode.NORMAL
            ),
            MiniGame(
                id = "rock_paper_scissors",
                title = "Rock Paper Scissors Tournament",
                description = "Everyone plays a knockout tournament of Rock Paper Scissors. Loser drinks.",
                type = MiniGameType.SELECTION,
                gameMode = GameMode.NORMAL
            )
        )

        // Add game mode specific mini games
        val modeGames = when(gameMode) {
            GameMode.CAMIONERO -> listOf(
                MiniGame(
                    id = "truck_stop",
                    title = "Truck Stop",
                    description = "Everyone holds a heavy object with outstretched arms. First to drop it drinks THREE times. Truckers need strength!",
                    type = MiniGameType.PHYSICAL,
                    gameMode = GameMode.CAMIONERO
                ),
                MiniGame(
                    id = "roadside_assistance",
                    title = "Roadside Assistance",
                    description = "Players race to mime changing a tire. Last to finish drinks TWICE. Truckers need these skills!",
                    type = MiniGameType.PHYSICAL,
                    gameMode = GameMode.CAMIONERO
                ),
                MiniGame(
                    id = "red_light_green_light",
                    title = "Traffic Control",
                    description = "Leader calls out 'Red Light', 'Green Light', or 'Yellow Light'. Players must stop, go, or slow down. Wrong moves drink TWICE.",
                    type = MiniGameType.REACTION,
                    gameMode = GameMode.CAMIONERO
                ),
                MiniGame(
                    id = "cargo_loading",
                    title = "Cargo Loading",
                    description = "Stack items (coins, cards, etc.) without them falling. Loser drinks THREE times. Careful with that cargo!",
                    type = MiniGameType.PHYSICAL,
                    gameMode = GameMode.CAMIONERO
                ),
                MiniGame(
                    id = "highway_code",
                    title = "Highway Code",
                    description = "Answer truck driving or road sign trivia. Wrong answers drink TWICE. Know your road rules!",
                    type = MiniGameType.CHALLENGE,
                    gameMode = GameMode.CAMIONERO
                )
            )

            GameMode.DESPECHADO -> listOf(
                MiniGame(
                    id = "emotional_playlist",
                    title = "Emotional Playlist",
                    description = "Go around naming songs about heartbreak. Repeats or blanks drink twice. Music heals the soul!",
                    type = MiniGameType.CHALLENGE,
                    gameMode = GameMode.DESPECHADO
                ),
                MiniGame(
                    id = "tissue_pass",
                    title = "Tissue Pass",
                    description = "Pass around an imaginary tissue box, each taking one and sharing a sad story. Most heartbreaking story chooses who drinks.",
                    type = MiniGameType.CHALLENGE,
                    gameMode = GameMode.DESPECHADO
                ),
                MiniGame(
                    id = "ex_factor",
                    title = "Ex Factor",
                    description = "Players briefly describe their worst ex. Group votes whose ex was the worst, winner gives out drinks equal to their heartbreaks.",
                    type = MiniGameType.SELECTION,
                    gameMode = GameMode.DESPECHADO
                ),
                MiniGame(
                    id = "heartbreak_hotel",
                    title = "Heartbreak Hotel",
                    description = "Each player acts out being dramatically heartbroken. Worst actor drinks twice. Channel your inner telenovela star!",
                    type = MiniGameType.PHYSICAL,
                    gameMode = GameMode.DESPECHADO
                ),
                MiniGame(
                    id = "message_regrets",
                    title = "Message Regrets",
                    description = "Share a text you wish you hadn't sent to an ex or drink twice. We've all been there!",
                    type = MiniGameType.CHALLENGE,
                    gameMode = GameMode.DESPECHADO
                )
            )

            GameMode.CALENTURIENTOS -> listOf(
                MiniGame(
                    id = "spin_the_bottle",
                    title = "Spin the Bottle",
                    description = "Spin a bottle. Whoever it points to must answer a spicy question or perform a flirty dare chosen by the spinner.",
                    type = MiniGameType.CHALLENGE,
                    gameMode = GameMode.CALENTURIENTOS
                ),
                MiniGame(
                    id = "hot_potato",
                    title = "Hot Potato",
                    description = "Pass an object around quickly. When the music stops, holder must give out a flirty dare or take two drinks.",
                    type = MiniGameType.REACTION,
                    gameMode = GameMode.CALENTURIENTOS
                ),
                MiniGame(
                    id = "body_art",
                    title = "Body Art",
                    description = "Draw a word on another player's arm with your finger, they guess what you wrote. Wrong guesses mean drinks for both.",
                    type = MiniGameType.PHYSICAL,
                    gameMode = GameMode.CALENTURIENTOS
                ),
                MiniGame(
                    id = "seven_minutes",
                    title = "Eye Contact",
                    description = "Two players must maintain eye contact for 30 seconds without laughing or looking away. Failures drink twice.",
                    type = MiniGameType.CHALLENGE,
                    gameMode = GameMode.CALENTURIENTOS
                ),
                MiniGame(
                    id = "seduction",
                    title = "Seduction Scene",
                    description = "Act out seducing an imaginary person using only three objects nearby. Group votes worst performance, who drinks.",
                    type = MiniGameType.PHYSICAL,
                    gameMode = GameMode.CALENTURIENTOS
                )
            )

            GameMode.MEDIA_COPAS -> listOf(
                MiniGame(
                    id = "slow_sips",
                    title = "Slow Sips",
                    description = "Everyone takes turns taking the smallest sip possible. Biggest sip takes another tiny sip. Stay hydrated!",
                    type = MiniGameType.CHALLENGE,
                    gameMode = GameMode.MEDIA_COPAS
                ),
                MiniGame(
                    id = "gentle_toss",
                    title = "Gentle Toss",
                    description = "Toss a light object (napkin, bottle cap) between players. Dropping only means a very small sip. Take it easy!",
                    type = MiniGameType.PHYSICAL,
                    gameMode = GameMode.MEDIA_COPAS
                ),
                MiniGame(
                    id = "quiet_game",
                    title = "Quiet Game",
                    description = "Everyone stays quiet. First to make a sound takes a small sip. Perfect for relaxing!",
                    type = MiniGameType.CHALLENGE,
                    gameMode = GameMode.MEDIA_COPAS
                ),
                MiniGame(
                    id = "steady_hand",
                    title = "Steady Hand",
                    description = "Balance a coin on your elbow then try to catch it. Failure means a tiny sip. No need to rush!",
                    type = MiniGameType.PHYSICAL,
                    gameMode = GameMode.MEDIA_COPAS
                ),
                MiniGame(
                    id = "light_count",
                    title = "Light Count",
                    description = "Count to 10 in different languages. Mistakes mean just a small sip. Learning is fun!",
                    type = MiniGameType.CHALLENGE,
                    gameMode = GameMode.MEDIA_COPAS
                )
            )

            else -> emptyList()
        }

        return defaultGames + modeGames
    }
}