package com.benchopo.firering.data

import com.benchopo.firering.model.*

object GameRulesProvider {

    fun getDefaultJackRules(gameMode: GameMode = GameMode.NORMAL): List<JackRule> {
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
                    id = "drunk_stories",
                    title = "Drunk Stories",
                    description = "Tell your most embarrassing drunk story or drink twice.",
                    type = RuleType.STANDARD,
                    gameMode = GameMode.CAMIONERO
                )
            )
            GameMode.DESPECHADO -> listOf(
                JackRule(
                    id = "ex_confessions",
                    title = "Ex Confessions",
                    description = "Share something about your ex or drink three times.",
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
                )
            )
            else -> emptyList()
        }

        return defaultRules + modeRules
    }

    fun getDefaultMiniGames(gameMode: GameMode = GameMode.NORMAL): List<MiniGame> {
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
            GameMode.MEDIA_COPAS -> listOf(
                MiniGame(
                    id = "rhyme_time",
                    title = "Rhyme Time",
                    description = "Say a word, then go around the circle with everyone saying a word that rhymes. First to fail drinks twice.",
                    type = MiniGameType.CHALLENGE,
                    gameMode = GameMode.MEDIA_COPAS
                )
            )
            GameMode.CALENTURIENTOS -> listOf(
                MiniGame(
                    id = "spin_the_bottle",
                    title = "Spin the Bottle",
                    description = "Spin the bottle. Whoever it points to must answer a spicy question or drink.",
                    type = MiniGameType.CHALLENGE,
                    gameMode = GameMode.CALENTURIENTOS
                )
            )
            else -> emptyList()
        }

        return defaultGames + modeGames
    }
}