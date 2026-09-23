package net.mcreator.thebackwoods.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class LogSplinterOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (Math.random() < 0.015) {
			String[] formerPlayers = {"+_+", "-_-", "._.", "^_^", "_-_", "_=_", "$Money$", "0_0", "_0ft3n_", "_0rd3r_", "_0ught_", "_0urs_", "_0utc0m3_", "_1_", "123456789_", "1337_h4x0r", "_1llus10n_", "_1mp4ct_", "_1nflu3nc3_", "_1nt3ll1g3nc3_",
					"_1nt3nt10n_", "_1t_", "1tap", "_1ts_", "1v1_me_bro", "2tap", "_3cl1ps3_", "_3ff3ct_", "<3_Gamer_<3", "_3l3ctr0_", "_3n3rgy_", "_3nd_", "_3v3rywh3r3_", "_3x4ctly_", "_4b0ut_", "_4byss_", "_4ct10n_", "_4ctu4l_", "_4g1l1ty_",
					"_4lw4ys_", "_4ng3r_", "_4nt1m4tt3r_", "_4nywh3r3_", "_4ppr0x1m4t3ly_", "_4r0und_", "_4sk_", "_4t0m1c_", "4utumn", "69_420_blaze_it", "_a3r0_", "A3ro", "a6d", "Ab0ut", "Abys5", "_Accepted_", "Act1on", "_Active_", "_Added_",
					"_Admin_", "admin_pls_no_ban", "_Advanced_", "Adventure_God", "_adventurer_", "Ag1lity", "_Agility_", "aimbot_exe", "_Air_", "Akinsoft", "_alchemist_", "_alien_", "_Allowed_", "alpha_wolf", "Alw4ys", "AnchorPVP_", "&And&",
					"Ang3r", "_angel_", "~Angel~", "_Anger_", "anime_lover_123", "_Anonymous_", "Ant1matter", "Antfrost", "Anywh3re", "Appr0ximately", "_Approved_", "Ar0und", "_archer_", "_artificer_", "Ask_", "_assassin_", "assembled_uniquely",
					"Astelic", "At0mic", "_Attacked_", "_Aura", "_Away_", "Awesamdude", "AxePVP_", "Axtual", "_b10_", "B1o", "_b3_", "_b3g1nn1ng_", "B3ginning", "/Backslash/", "BadBoyHalo", "BajanCanadian", "_Banned_", "_barbarian_", "_bard_",
					"_Basic_", "BastiGHG", "bath_water_salesman", "batman_arkham", "Bdubs", "Be_", "BedlessNoob", "BedWars_God", "_blacksmith_", "_Blocked_", "BlockPlacer_", "blxssom", "Boffy", "!Boom!", "_Bot_", "BowPVP_", "_broken_heart_",
					"_Bugha_", "_builder_", "_Builder_", "built_different", "built_incorrectly", "_Busy_", "bxnny", "_c0m3_", "C0me", "_c0ns3qu3nc3_", "C0nsequence", "_c0nst1tut10n_", "C0nstitution", "_c0sm1c_", "C0smic", "_c0uld_", "C0uld",
					"_c0ur4g3_", "C0urage", "_c4ll_", "C4ll", "_c4n_", "C4n", "_c4us3_", "C4use", "CallMeKevin", "captain_america_fan", "CaptainSparklez", "_Caught_", "_ch40s_", "Ch4os", "_ch4r1sm4_", "Ch4risma", "_Chad_", "_Challenger_",
					"_Champion_", "_Changed_", "_Chaotic_", "_Charisma_", "Chazm", "_chef_", "chicken_nugget_lover", "_chr0n0_", "Chr0no", "_cl0s3_", "Cl0se", "_cleric_", "_Clicked_", "_Common_", "_Completed_", "_Constitution_",
					"constructed_alternatively", "coolkid2008", "cope_harder", "_Countered_", "_Courage_", "CPK", "_crafter_", "_Crawled_", "_Created_", "Creative_God", "_Creator_", "Creeper_Aww_Man", "_Crouched_", "_cru3lty_", "Cru3lty", "_cry0_",
					"Cry0", "CrystalPVP_", "Cubfan135", "cxloud", "Cxlvxn", "_cyb3r_", "Cyb3r", "_cyborg_", "_d0_", "_d13s3l_", "D1esel", "_d1m3ns10n4l_", "D1mensional", "_d1st4nt_", "D1stant", "_d3st1ny_", "D3stiny", "_d3xt3r1ty_", "D3xterity",
					"_d4rk_3n3rgy_", "D4rk_Energy", "_d4rk_m4tt3r_", "D4rk_Matter", "_d4wn_", "D4wn", "_Damaged_", "DanTDM", "Dante", "_Darkness_", "darth_vader_fan", "Deadmau5", "_Defended_", "Defone", "_Deleted_", "_demon_", "-Demon-", "_Denied_",
					"_Despair_", "_Despawned_", "_Destiny_", "_Developer_", "_Dexterity_", "Diamond_Sword_PVP", "didnt_ask", "_Died_", "dilate", "_Dimension_", "Dinnerbone", "Do_", "Docm77", "_Dodged_", "_Do_Not_Disturb_", "_Double_Clicked_",
					"_Dragged_", "_dragon_", "_Dream_", "Dream", "_Drew_", "_Dropped_", "_druid_", "_dusk_", "Dusk_", "dxpressed", "dxrk", "E1ectro", "_Earth_", "Eclips3", "Eff3ct", "ElRichMC", "En3rgy", "End_", "_Epic_", "_Epic_Gamer_", "=Equals=",
					"_Equipped_", "Eret", "Etho", "EthosLab", "Ev3rywhere", "_Evil_", "Ex4ctly", "_Executed_", "_Expert_", "_explorer_", "ez_clap", "_f0r3v3r_", "F0rever", "_f1nd_", "F1nd", "_f33l_", "_f34r_", "F3ar", "F3el", "_f4nt4sy_", "F4ntasy",
					"_f4r_", "F4r", "_f4t3_", "F4te", "_Failed_", "_Fake_", "_False_", "FalseSymmetry", "_farmer_", "_Fate_", "_Fear_", "_Fell_", "_Finished_", "_Fire_", "FireLord_98", "_fisherman_", "_Flew_", "_Forbidden_", "_Forgiven_",
					"Fruitberries", "Fundy", "Furball", "_futur3_", "Futur3", "fxiry", "fxrgiven", "_g0_", "_g1v3_", "G1ve", "_g30_", "_g3n3r4lly_", "G3nerally", "G3o", "_g3t_", "G3t", "g3t_g00d", "_g4l4ct1c_", "G4lactic", "_Galaxy_", "Gamerboy80",
					"GamerGirl_99", "GamingWithJen", "_gatherer_", "gb80", "Geni", "GeorgeNotFound", "GermanLetsPlay", "get_rekt", "gG_wP", "_ghost_", "_Ghost_Rider_", "_Gigachad_", "gL_hF", "_Glided_", "Go_", "_god_", "GodBridger_", "_God_Gamer_",
					"Goku_SuperSaiyan", "_Good_", "GoodTimesWithScar", "_gr4v1ty_", "Gr4vity", "_Grandmaster_", "_Granted_", "Grian", "Grumm", "_Guest_", "Guude", "_h1m_", "H1m", "_h1s_", "H1s", "_h3_", "_h3r_", "H3r", "_h3r3_", "H3re", "_h3rs_",
					"H3rs", "_h4pp1n3ss_", "H4ppiness", "_h4v3_", "H4ve", "_hacker_", "Hannahxxrose", "Hardcore_God", "harley_quinn_fan", "_Hate_", "HBomb94", "He_", "_Healed_", "_Held_", "_Helper_", "_hero_", "_Hero_", "_Hidden_", "_Hit_", "_Hope_",
					"Huahwi", "_hunter_", "_hydr0_", "Hydr0", "Hypno", "I_", "I11usion", "I_am_Batman_007", "i_am_speed", "iBallisticSquid", "_Ice_", "idk_my_name", "_Idle_", "_I_Dont_Know_", "i_have_hacks", "i_i", "ii_", "iiTz_", "iJevin",
					"i_like_turtles", "Illumina", "_Illusion_", "i_miss_her", "Imp4ct", "ImpulseSV", "_Inactive_", "Influ3nce", "Int3lligence", "Int3ntion", "IntelEdits", "_Intelligence_", "_Interacted_", "InTheLittleWood", "_Invisible_", "iq_",
					"_iron_man_", "Iskall85", "It_", "Its_", "ItsFundy", "itz_me_mario", "JackManifoldTV", "Jacksepticeye", "jeb_", "JeromeASF", "Jiub", "_Joy_", "_ju5t1c3_", "_Jumped_", "Just1ce", "Just_A_Random_Guy", "just_vibing_tbh", "Jxpiter",
					"_k1n3t1c_", "K1netic", "k1tty", "_k4rm4_", "K4rma", "kachow", "KarlJacobs", "_Karma_", "kawaii_desu", "kekw", "_Kept_", "Keralis", "_Kicked_", "_Killed_", "_king_", "_kn0w_", "Kn0w", "_kn0wl3dg3_", "Kn0wledge", "_knight_",
					"_Knowledge_", "Kxrma", "_l00k_", "L0nely", "L0ok", "_l34v3_", "L3ave", "_l4w_", "L4w", "Lachlan", "_Lawful_", "Lck", "LDShadowLady", "_Left_Clicked_", "_legend_", "_Legendary_", "_Lies_", "_Life_", "_Light_", "_Lightning_",
					"lll_Ill_lll", "_Loaded_", "Logdotzip", "_lone_survivor_", "LoneWolf_Alpha", "_Lost_", "_Love_", "_luck_", "_Luck_", "luke_skywalker_69", "LukeTheNotable", "_lumberjack_", "_lun4r_", "Lun4r", "luv", "luvs_", "lxner", "lxve",
					"m00n", "M00nlight", "_m0rn1ng_", "_m1ddl3_", "M1ddle", "_m1dn1ght_", "_m1ght_", "M1ght", "_m1n3_", "M1ne", "_m3_", "_m3ch_", "M3ch", "_m3rcy_", "M3rcy", "_m4g1c_", "M4gic", "_m4gn3t1sm_", "M4gnetism", "_m4k3_", "M4ke",
					"_m4tt3r_", "M4tter", "_m4y_", "M4y", "_mage_", "mald", "Markiplier", "Martyn", "_master_", "_Master_", "_Maybe_", "Me_", "Mefs", "MegaPVP", "_Middle_Clicked_", "_miner_", "_Missed_", "_MLG_Pro_", "_Mod_", "_Moderator_",
					"_Modified_", "_monk_", "monkaS", "_monster_", "_Moon_", "MooseCraft", "_Moved_", "MrBeast", "Mrcy", "MumboJumbo", "_must_", "Must_", "_mutant_", "_Muted_", "_MVP_", "_MVP_Plus_", "_MVP_Plus_Plus_", "Mxrs", "_Myth_", "_Mythic_",
					"n00b_sl4y3r", "_n00n_", "_n0wh3r3_", "N0where", "_n1ght_", "_n34r_", "N3ar", "_n3cr0_", "N3cro", "N3ptune", "_n3v3r_", "N3ver", "_Naruto_Uzumaki_", "_necromancer_", "_Neutral_", "_Nightmare_", "Nihachu", "@Ninja@", "ninja_hyper",
					"_No_", "_noob_", "Noob_Destroyer_99", "(Noob)_Killer", "_Noob_Master_", "NoobMaster69", "Not_A_Bot_I_Swear", "Notch", "NotNico", "_NPC_", "_nucl34r_", "Nucl3ar", "Nxtune", "_Official_", "_Offline_", "Oft3n", "_Online_", "o_o",
					"o_O", "O_O", "Optic_Scump_Fan", "Ord3r", "OrionSound", "Ought_", "Ours_", "Outc0me", "_Owner_", "_p0t3nt14l_", "P0tential", "_p0w3r_", "P0wer", "_p1ty_", "P1ty", "_p41n_", "P4in", "_p4st_", "P4st", "_paladin_", "Paluten",
					"_Pardoned_", "_Parried_", "_Partner_", "_Paused_", "PauseUnpause", "Pepega", "%Percent%", "PeteZahHutt", "PewDiePie", "_ph0t0_", "Ph0to", "Philza", "_Picked_Up_", "|Pipe|", "_pl34sur3_", "Pl3asure", "_Player_", "Player_1_Ready",
					"Player_991", "+Plus+", "Plut0", "Plxto", "pogchamp", "poggies", "PopularMMOs", "potato_master", "_Power_", "_pr3s3nt_", "Pr3sent", "_Pressed_", "PrestonPlayz", "_prince_", "_princess_", "_pro_", "_Pro_Gamer_", "ProNoob123",
					"[Pro]_Player", "_Pro_Slayer_", "_psy_", "Psyk0", "Punz", "Purpled", "pvp_god", "PVP_Sweat", "pwn3d_u", "pxin", "pxtal", "_pyr0_", "Pyr0", "q_p", "qq_", "qrx", "_qu4ntum_", "Qu4ntum", "Quackity", "_queen_", "qWither",
					"_r34ct10n_", "_r34l1ty_", "R3action", "R3ality", "r3kt", "_r3l4t1v1ty_", "R3lativity", "_r3sult_", "R3sult", "_r4d14t10n_", "R4diation", "_r4r3ly_", "R4rely", "_Rain_", "_Ran_", "Ranboo", "_ranger_", "_Rare_", "ratio_plus_L",
					"rawr_xd", "_Read_", "_Real_", "_Reality_", "refraction", "_Rejected_", "_Released_", "_Removed_", "Rendog", "_Reported_", "_Respawned_", "_Resumed_", "_Revived_", "_Revoked_", "_Right_Clicked_", "Rky", "_robot_", "_rogue_",
					"rrx", "RTGame", "Rubius", "rxse", "_s0l4r_", "S0lar", "_s0m3t1m3s_", "_s0m3wh3r3_", "S0metimes", "S0mewhere", "_s33_", "_s33m_", "S3e", "S3em", "_s4dn3ss_", "S4dness", "_s4y_", "S4y", "Sad_Boy_Hours", "_salty_", "Sammyuri",
					"Sapnap", "Sasuke_Uchiha_69", "_Saved_", "sb737", "_sc1_f1_", "Sc1_F1", "_Scored_", "_Scrolled_", "Seapeekay", "_Secret_", "seethe", "senpai_notice_me", "_sh0uld_", "Sh0uld", "_sh3_", "_sh4ll_", "Sh4ll", "_S_H_A_D_O_W_",
					"_ShadowNinja_", "She_", "Shubble", "shxdow", "sigma_male_grindset", "Silent_Assassin7", "_Simp_", "simply_better", "Sipover", "sith_lord_99", "_skeleton_", "Skeppy", "skill_issue", "SkyWars_God", "\\Slash\\", "Slimecicle",
					"Smajor1995", "SmallishBeans", "s_mmer", "_Sneaked_", "_Sniper", "_Snow_", "SolidarityGaming", "_sorcerer_", "_Sorrow_", "_sp33d_", "_sp3c1f1c4lly_", "Sp3cifically", "Sp3ed", "_Space_", "_Spawned_", "Spectator_God",
					"specularpotato", "_Speed_", "spider_man_ps4", "Spifey", "Spintown", "_spirit_", "spr1ng", "_Sprinted_", "SSundee", "_st34m_", "St3am", "_st3ll4r_", "Stampylonghead", "*Star*", "_Star_", "_Started_", "StimpyPvP", "_Stopped_",
					"_Storm_", "_str3ngth_", "Str3ngth", "_Streamer_", "_Strength_", "Stressmonster101", "stxr", "Stxrry", "_Succeeded_", "SuchSpeed", "_summoner_", "_Sun_", "Sunr1se", "Suns3t", "_superhero_", "_supervillain_", "Survival_God",
					"#Swag#", "_Swam_", "_sweat_", "Sweat.exe", "Sweaty_Gamer", "SweatyTryhard69", "SwordPVP_", "sxar", "sxlk", "sxnner", "Sxturn", "Sylvee", "_t0_", "_t3ch_", "T3ch", "_t3l3_", "T3le", "_t3ll_", "T3ll", "_t4k3_", "T4ke", "TangoTek",
					"TapL", "Target3dGaming", "Technoblade", "_Teleported_", "Tenebrous", "Tfue_fanboy", "_th0s3_", "Th0se", "_th1nk_", "Th1nk", "_th1s_", "Th1s", "_th31rs_", "Th3irs", "_th3m_", "Th3m", "_th3r3_", "Th3re", "_th3s3_", "Th3se",
					"_th3y_", "Th3y", "_th4t_", "Th4t", "the_last_jedi", "TheReal_JohnDoe", "_thief_", "ThirtyVirus", "_Threw_", "_Thunder_", "`Tick`", "_Tied_", "~Tilde~", "_Time_", "TimeDeo", "To_", "TommyInnit", "touch_grass", "Tox1c_K1d",
					"Tox1c_Viper", "_toxic_", "_trapper_", "trolololol", "_True_", "_Truth_", "_try_", "Try_", "_tryhard_", "Tryhard_PvP", "Tubbo", "Tubbo_", "_tw1l1ght_", "Twil1ght", "_Twitch_", "_Tyler1_Alpha_", "_Typed_", "UHC_God", "u_mad_bro",
					"_un1v3rs4l_", "Un1versal", "_Unbanned_", "_Uncommon_", "_Underscore_", "_Unequipped_", "uninstall_plz", "_Universe_", "_Unknown_", "_Unmuted_", "UnspeakableGaming", "^Up^", "_Updated_", "Ur4nus", "ur_bad", "_us_", "Us_", "_us3_",
					"Us3", "_Used_", "_User_", "user_19847291", "_usu4lly_", "Usu4lly", "uwu_owo", "_v01d_", "V0id", "_v1rtu4l_", "V1rtual", "v3lvet", "_vampire_", "Vegeta777", "_Verified_", "Vikkstar", "Vikkstar123", "_villain_", "_Villain_",
					"VintageBeef", "_VIP_", "_Visible_", "vKxrma", "__void__", "vqmp", "vv_", "Vxnus", "Vxrtex", "_vX_xV_", "_w0rk_", "W0rk", "_w0uld_", "W0uld", "_w1ll_", "W1ll", "w1nter", "_w1sd0m_", "W1sdom", "_w3_", "_w4nt_", "W4nt", "WadZee",
					"_Walked_", "Wallibear", "_warlock_", "_Warned_", "_warrior_", "_Water_", "We_", "Welsknight", "_werewolf_", "?WhoAmI?", "WilburSoot", "_Wind_", "_Wisdom_", "Wisp", "_wizard_", "_Won_", "_Written_", "xBCrafted", "xHqrd",
					"Xisumavoid", "xLemoN", "x_Luv_x", "xNestorio", "xQcOW_fan", "xRpMx13", "xShatter", "xTurtle", "x_x", "xX_1v1_Me_Xx", "xX_DarkSlayer_Xx", "xX_Death_Xx", "xX_Demon_Slayer_Xx", "_xX_Demon_Xx_", "xX_EdgyTeen_Xx", "_Xx_Edgy_xX_",
					"_xX_FaZe_Xx_", "xX_G0D_Xx", "xX_Gamer_Girl_Xx", "_xX_Gamer_Xx_", "xX_Joker_Xx", "xX_NoScope_Xx", "xX_Otaku_Xx", "_xX_Skill_Issue_Xx_", "xX_Slayer_Xx", "x_X_Sniper_X_x", "Xx_SweatyTryhard_xX", "xX_wAk3_uP_Xx", "xXx_DemonGod_xXx",
					"xZet", "_y0u_", "Y0u", "_y0urs_", "Y0urs", "yeet_or_be_yeeted", "_Yes_", "_YouTube_", "_YT_", "Zedaph", "Zelkam", "zLxve", "_zombie_", "zxq", "Zyph", "zZz_sL33pY_zZz", "xX_PVP_Master_Xx", "Sweaty_Tryhard99", "Diamond_God",
					"Redstone_Wizard", "Creeper_Hunter", "Ender_Slayer", "Nether_King", "Void_Walker", "Star_Catcher", "Moon_Child", "Sun_Praiser", "Storm_Bringer", "Shadow_Weaver", "Light_Bringer", "Flame_Wielder", "Frost_Mage", "Earth_Shaker",
					"Wind_Rider", "Water_Bender", "Noob_Slayer99", "xX_Pro_Gamer_Xx", "Minecraft_God", "Diamond_Sword", "Creeper_Aww_Man", "Notch_Fan", "Jeb_Fan", "Dinnerbone_Fan", "Herobrine_Fan", "Entity_303", "Null_Entity", "Green_Steve",
					"Far_Lands_Explorer", "Sky_Grid_Master", "Sky_Block_Pro", "Survival_Games_Champ", "Hunger_Games_Winner", "Bed_Wars_God", "Sky_Wars_Pro", "Egg_Wars_Master", "Money_Wars_King", "Murder_Mystery_Detective", "Build_Battle_Winner",
					"UHC_Champion", "Factions_King", "Prison_Guard", "Creative_Builder", "Survival_Expert", "Hardcore_Veteran", "Touch_Grass_Pls", "i_have_no_friends", "ur_bad_kid", "ez_pz_lemon_sqz", "gg_no_re", "get_good_kid", "skill_issue_101",
					"cope_and_seethe", "mald_harder", "ratioed_by_a_noob", "didnt_ask_plus_L", "L_bozo", "ur_mom_lol", "your_trash_kid", "delete_the_game", "uninstall_now", "go_play_roblox", "minecraft_is_better", "fortnite_is_trash",
					"roblox_is_for_kids", "terraria_is_good", "minecraft_veteran", "og_minecrafter", "2011_player", "alpha_tester", "beta_tester", "infdev_player", "indev_player", "classic_player", "survival_test_player", "pre_classic_player",
					"cave_game_player", "ruby_miner", "emerald_trader", "villager_scammer", "iron_golem_killer", "snow_golem_builder", "wither_summoner", "ender_dragon_slayer", "warden_hunter", "elder_guardian_killer", "ravager_rider",
					"evoker_summoner", "vindicator_axeman", "pillager_crossbowman", "illusioner_blinder", "witch_brewer", "phantom_flyer", "drowned_swimmer", "husk_walker", "stray_archer", "stxr_gazer", "vxid_walker", "lxnar_eclipse", "sxlar_flare",
					"nxva_star", "cxsmic_dust", "gxalaxy_quest", "nxbulous_cloud", "axstral_projection", "cxelestial_being", "Ghost_Ninja", "Phantom_Slayer", "Ender_Mage", "Pixel_God", "Block_Breaker", "Iron_Golem", "Diamond_Miner", "Lava_Swimmer",
					"Redstone_Genius", "TNT_Lover", "Creeper_Hugger", "Pigman_Fighter", "Zombie_Hunter", "Skeleton_Archer", "Spider_Rider", "Slime_Bouncer", "Enderman_Stater", "Dragon_Tamer", "Wither_Warrior", "Blaze_Rod", "Alterna43", "SlateBrick",
					"LivingChili", "Drosera_Amethyst", "Ghast_Tear", "Magma_Cube", "Silverfish_Swarm", "Witch_Doctor", "Bat_Man", "Villager_Trader", "Iron_Sword", "Gold_Pickaxe", "Diamond_Armor", "Nether_Portal", "End_City", "Stronghold_Finder",
					"Mineshaft_Explorer", "Dungeon_Looter", "Temple_Raider", "Village_Hero", "Raid_Captain", "Pillager_Outpost", "Ocean_Monument", "Woodland_Mansion", "Jungle_Temple", "Desert_Pyramid", "Igloo_Dweller", "Swamp_Hut", "Nether_Fortress",
					"Bastion_Remnant", "Ruined_Portal", "Shipwreck_Survivor", "Buried_Treasure", "Amethyst_Geode", "Lush_Cave", "Dripstone_Cave", "Deep_Dark", "Ancient_City", "Warden_Slayer", "Wither_Skeleton", "Piglin_Brute", "Hoglin_Hunter",
					"Strider_Rider", "Zoglin_Tamer", "NoobMaster77", "Sweaty_PVP", "GodBridge_Pro", "Block_Placer99", "BedWars_Tryhard", "SkyWars_Sweat", "UHC_Veteran", "Survival_Pro", "Creative_Master", "Redstone_Engineer"};
			entity.getPersistentData().putString("SplinterUsername", formerPlayers[(int) (Math.random() * formerPlayers.length)]);
		}
		if (entity instanceof LivingEntity _livingEntity0 && _livingEntity0.getAttributes().hasAttribute(Attributes.SAFE_FALL_DISTANCE))
			_livingEntity0.getAttribute(Attributes.SAFE_FALL_DISTANCE).setBaseValue(8);
		if (entity instanceof LivingEntity _livingEntity1 && _livingEntity1.getAttributes().hasAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY))
			_livingEntity1.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).setBaseValue(0.4);
	}
}