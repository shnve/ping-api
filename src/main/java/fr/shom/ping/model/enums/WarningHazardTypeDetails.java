package fr.shom.ping.model.enums;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WarningHazardTypeDetails {
    ACOUSTIC_RECORDER(0, newCombinations(), 1),
    AIS_TRANSMITTER_OPERATING_PROPERLY(0, createCombinations(newCombination().withIncidents(83, 99, 103).withNature(3)), 4),
    AIS_TRANSMITTER_OUT_OF_SERVICE(0, createCombinations(newCombination().withIncident(83).withNatures(1, 2)), 5),
    AIS_TRANSMITTER_REMOVAL(37, newCombinations(), 6),
    AIS_TRANSMITTER_TEMPORARY_REMOVAL(38, createCombinations(newCombination().withIncident(99).withNatures(1, 2)), 7),
    AIS_TRANSMITTER_UNREALIABLE(0, createCombinations(newCombination().withIncident(103).withNatures(1, 2)), 8),
    AIS_TRASMITTER_ESTABLISHMENT(8, newCombinations(), 3),
    AIS_TRASMITTER_TEMPORARY_ESTABLISHMENT(50, newCombinations(), 2),
    ALL_AIDS_TO_NAVIGATION_NOT_RELIABLE(0, newCombinations(), 9),
    ANTI_POLLUTION_EXERCISE(0, newCombinations(), 10),
    ANTI_POLLUTION_OPERATION(0, newCombinations(), 11),
    AQUACULTURE_SITE(0, newCombinations(), 12),
    ATON_OPERATING_PROPERLY(0, createCombinations(newCombination().withIncidents(0).withNature(3)), 13),
    AUDIBLE_SIGNAL_CHANGE(21, newCombinations(), 14),
    AUDIBLE_SIGNAL_ESTABLISHMENT(6, newCombinations(), 15),
    AUDIBLE_SIGNAL_OPERATING_PROPERLY(0, createCombinations(newCombination().withIncidents(81, 91, 98).withNature(3)), 16),
    AUDIBLE_SIGNAL_OUT_OF_SERVICE(0, createCombinations(newCombination().withIncident(81).withNatures(1, 2)), 17),
    AUDIBLE_SIGNAL_REMOVAL(35, newCombinations(), 18),
    AUDIBLE_SIGNAL_TEMPORARY_CHANGE(22, createCombinations(newCombination().withIncident(91).withNatures(1, 2)), 19),
    AUDIBLE_SIGNAL_TEMPORARY_ESTABLISHMENT(48, newCombinations(), 20),
    AUDIBLE_SIGNAL_TEMPORARY_REMOVAL(36, createCombinations(newCombination().withIncident(98).withNatures(1, 2)), 21),
    AUTHORIZED_ICE_ROUTING_INFORMATION(0, newCombinations(), 22),
    BEACON_CHANGE(17, newCombinations(), 23),
    BEACON_DAMAGED(0, createCombinations(newCombination().withIncident(62).withNatures(1, 2)), 24),
    BEACON_DAYMARK_UNRELIABLE(0, createCombinations(newCombination().withIncident(69).withNatures(1, 2)), 25),
    BEACON_ESTABLISHMENT(5, newCombinations(), 26),
    BEACON_MISSING(0, createCombinations(newCombination().withIncident(61).withNatures(1, 2)), 27),
    BEACON_REMOVAL(31, newCombinations(), 28),
    BEACON_RESTORED_TO_NORMAL(0, createCombinations(newCombination().withIncidents(61, 62, 63, 64, 65, 66, 67, 68, 69, 89, 96).withNature(3)), 29),
    BEACON_TEMPORARY_CHANGE(18, createCombinations(newCombination().withIncident(89).withNatures(1, 2)), 30),
    BEACON_TEMPORARY_ESTABLISHMENT(47, newCombinations(), 31),
    BEACON_TEMPORARY_REMOVAL(32, createCombinations(newCombination().withIncident(96).withNatures(1, 2)), 32),
    BEACON_TOPMARK_DAMAGED(0, createCombinations(newCombination().withIncident(68).withNatures(1, 2)), 33),
    BEACON_TOPMARK_MISSING(0, createCombinations(newCombination().withIncident(67).withNatures(1, 2)), 34),
    BLASTING_OPERATION(0, newCombinations(), 35),
    BREAKWATER_CONSTRUCTION(0, newCombinations(), 36),
    BRIDGE_HORIZ_CLEAR_CHG(0, newCombinations(), 37),
    BRIDGE_UNABLE_CLOSE(0, newCombinations(), 38),
    BRIDGE_UNABLE_OPEN(0, newCombinations(), 39),
    BRIDGE_VERT_CLEAR_CHG(0, newCombinations(), 40),
    BUOY_ADRIFT(0, createCombinations(newCombination().withIncident(49).withNatures(1, 2)), 41),
    BUOY_CHANGE(13, newCombinations(), 42),
    BUOY_DAMAGED(0, createCombinations(newCombination().withIncident(50).withNatures(1, 2)), 44),
    BUOY_DAYMARK_UNRELIABLE(0, createCombinations(newCombination().withIncident(53).withNatures(1, 2)), 45),
    BUOY_DECOMMISSIONED_FOR_WINTER(0, newCombinations(), 46),
    BUOY_DESTROYED(0, newCombinations(), 47),
    BUOY_ESTABLISHMENT(3, newCombinations(), 48),
    BUOY_LIFTED_FOR_WINTER(0, newCombinations(), 63),
    BUOY_MISSING(0, createCombinations(newCombination().withIncident(47).withNatures(1, 2)), 49),
    BUOY_MOVE(0, newCombinations(), 50),
    BUOY_OFF_POSITION(0, createCombinations(newCombination().withIncident(48).withNatures(1, 2)), 51),
    BUOY_RECOMMISSIONED_FOR_NAVIGATION_SEASON(0, newCombinations(), 43),
    BUOY_REMOVAL(new int[]{27, 63}, newCombinations(), 53),
    BUOY_REPLACED_BY_WINTER_SPAR(0, newCombinations(), 54),
    BUOY_RESTORED_TO_NORMAL(0, createCombinations(newCombination().withIncidents(43, 44, 45, 46, 50, 51, 52, 53, 87).withNature(3)), 55),
    BUOY_RE_ESTABLISHMENT(0, createCombinations(newCombination().withIncidents(47, 48, 49, 54, 94).withNature(3)), 52),
    BUOY_TEMPORARY_CHANGE(14, createCombinations(newCombination().withIncident(87).withNatures(1, 2)), 56),
    BUOY_TEMPORARY_ESTABLISHMENT(45, newCombinations(), 57),
    BUOY_TEMPORARY_REMOVAL(28, createCombinations(newCombination().withIncident(94).withNatures(1, 2)), 58),
    BUOY_TOPMARK_DAMAGED(0, createCombinations(newCombination().withIncident(52).withNatures(1, 2)), 59),
    BUOY_TOPMARK_MISSING(0, createCombinations(newCombination().withIncident(51).withNatures(1, 2)), 60),
    BUOY_WILL_BE_WITHDRAWN(0, newCombinations(), 61),
    BUOY_WITHDRAWN(0, createCombinations(newCombination().withIncident(54).withNatures(1, 2)), 62),
    CABLE_LAYING_OPERATION(0, newCombinations(), 64),
    CHAYKA_OPERATING_PROPERLY(0, newCombinations(), 66),
    CHAYKA_OUT_OF_SERVICE(0, newCombinations(), 67),
    CHAYKA_STATION_REMOVAL(0, newCombinations(), 68),
    CHAYKA_STATION_TEMPORARY_REMOVAL(0, newCombinations(), 69),
    CHAYKA_UNREALIABLE(0, newCombinations(), 70),
    CLUSTER_OF_FISHING_VESSELS(0, newCombinations(), 71),
    CONTAINER_ADRIFT(0, newCombinations(), 72),
    DANGEROUS_WRECK(0, newCombinations(), 73),
    DEAD_HEAD_ADRIFT(0, newCombinations(), 75),
    DEAD_WHALE_ADRIFT(0, newCombinations(), 74),
    DERELICT_VESSEL_ADRIFT(0, newCombinations(), 76),
    DGLONASS_OPERATING_PROPERLY(0, newCombinations(), 77),
    DGLONASS_OUT_OF_SERVICE(0, newCombinations(), 78),
    DGLONASS_STATION_ESTABLISHMENT(0, newCombinations(), 79),
    DGLONASS_UNREALIABLE(0, newCombinations(), 80),
    DGPS_OPERATING_PROPERLY(0, createCombinations(newCombination().withIncidents(86, 106, 107).withNature(3)), 81),
    DGPS_OUT_OF_SERVICE(0, createCombinations(newCombination().withIncident(86).withNatures(1, 2)), 82),
    DGPS_STATION_ESTABLISHMENT(11, newCombinations(), 83),
    DGPS_STATION_REMOVAL(43, newCombinations(), 84),
    DGPS_STATION_TEMPORARY_REMOVAL(0, createCombinations(newCombination().withIncident(107).withNatures(1, 2)), 85),
    DGPS_UNRELIABLE(0, createCombinations(newCombination().withIncident(106).withNatures(1, 2)), 86),
    DIVING_OPERATIONS(0, newCombinations(), 87),
    DOCK_ADRIFT(0, newCombinations(), 88),
    DREDGING_OPERATION(0, newCombinations(), 89),
    DRILLING_SITE_OPERATIONS(0, newCombinations(), 91),
    DRILL_RIG_UNDER_TOW(0, newCombinations(), 90),
    EGC_MSI_SERVICE(0, newCombinations(), 98),
    EGNOS_OPERATING_PROPERLY(0, newCombinations(), 99),
    EGNOS_OUT_OF_SERVICE(0, newCombinations(), 100),
    EGNOS_STATION_ESTABLISHMENT(12, newCombinations(), 101),
    EGNOS_STATION_REMOVAL(44, newCombinations(), 102),
    EGNOS_STATION_TEMPORARY_REMOVAL(0, newCombinations(), 103),
    EGNOS_UNREALIABLE(0, newCombinations(), 104),
    ELORAN_OPERATING_PROPERLY(0, newCombinations(), 105),
    ELORAN_OUT_OF_SERVICE(0, newCombinations(), 106),
    ELORAN_STATION_ESTABLISHMENT(0, newCombinations(), 107),
    ELORAN_STATION_REMOVAL(0, newCombinations(), 108),
    ELORAN_STATION_TEMPORARY_REMOVAL(0, newCombinations(), 109),
    ELORAN_UNREALIABLE(0, newCombinations(), 110),
    EXCLUSION_ZONES(0, newCombinations(), 111),
    EXPLOSIVE_DEVICE(0, newCombinations(), 112),
    E_CHAYKA_OPERATING_PROPERLY(0, newCombinations(), 92),
    E_CHAYKA_OUT_OF_SERVICE(0, newCombinations(), 93),
    E_CHAYKA_STATION_ESTABLISHMENT(0, newCombinations(), 94),
    E_CHAYKA_STATION_REMOVAL(0, newCombinations(), 95),
    E_CHAYKA_STATION_TEMPORARY_REMOVAL(0, newCombinations(), 96),
    E_CHAYKA_UNREALIABLE(0, newCombinations(), 97),
    FAIRWAY_MARKER_DAMAGED(0, newCombinations(), 116),
    FAIRWAY_MARKER_DESTROYED(0, newCombinations(), 117),
    FAIRWAY_MARKER_LIGHT_NOT_SYNCHRONIZED(0, newCombinations(), 113),
    FAIRWAY_MARKER_LIGHT_UNLIT(0, newCombinations(), 114),
    FAIRWAY_MARKER_LIGHT_UNRELIABLE(0, newCombinations(), 115),
    FALLOUT_HAZARD(0, newCombinations(), 118),
    FIREWORKS(0, newCombinations(), 119),
    FIRING_EXERCISE(0, newCombinations(), 120),
    FISH_AGGREGATING_DEVICE_FAD(0, newCombinations(), 121),
    FISH_NET_ADRIFT(0, newCombinations(), 122),
    FLOATING_DEBRIS(0, newCombinations(), 123),
    FLOODLIT_BEACON_UNLIT(0, newCombinations(), 124),
    FOG_SIGNAL_CHANGE(19, newCombinations(), 125),
    FOG_SIGNAL_ESTABLISHMENT(7, newCombinations(), 126),
    FOG_SIGNAL_OPERATING_PROPERLY(0, createCombinations(newCombination().withIncidents(82, 90, 97).withNature(3)), 127),
    FOG_SIGNAL_OUT_OF_SERVICE(0, createCombinations(newCombination().withIncident(82).withNatures(1, 2)), 128),
    FOG_SIGNAL_REMOVAL(33, newCombinations(), 129),
    FOG_SIGNAL_TEMPORARY_CHANGE(20, createCombinations(newCombination().withIncident(90).withNatures(1, 2)), 130),
    FOG_SIGNAL_TEMPORARY_ESTABLISHMENT(49, newCombinations(), 131),
    FOG_SIGNAL_TEMPORARY_REMOVAL(34, createCombinations(newCombination().withIncident(97).withNatures(1, 2)), 132),
    FRONT_LEADING_RANGE_BEACON_RESTORED_TO_NORMAL(0, createCombinations(newCombination().withIncident(79).withNature(3)), 134),
    FRONT_LEADING_RANGE_BEACON_UNRELIABLE(0, createCombinations(newCombination().withIncident(79).withNatures(1, 2)), 135),
    FRONT_LEADING_RANGE_LIGHT_IS_OPERATING_PROPERLY(0, createCombinations(newCombination().withIncidents(70, 72, 74, 76).withNature(3)), 136),
    FRONT_LEADING_RANGE_LIGHT_LIGHT_RANGE_REDUCED(0, createCombinations(newCombination().withIncident(74).withNatures(1, 2)), 137),
    FRONT_LEADING_RANGE_LIGHT_LIGHT_UNLIT(0, createCombinations(newCombination().withIncident(70).withNatures(1, 2)), 138),
    FRONT_LEADING_RANGE_LIGHT_UNRELIABLE(0, createCombinations(newCombination().withIncident(72).withNatures(1, 2)), 139),
    FRONT_LEADING_RANGE_LIGHT_WITHOUT_RHYTHM(0, createCombinations(newCombination().withIncident(76).withNatures(1, 2)), 140),
    GNSS_DEGRADATION(0, newCombinations(), 141),
    HAZARDOUS_AREA(0, newCombinations(), 142),
    HF_SERVICE(0, newCombinations(), 143),
    HIGH_WATER_LEVEL(0, newCombinations(), 144),
    HORIZ_CLEAR_REDUCED(0, newCombinations(), 145),
    HYDROGRAPHIC_SURVEY(0, newCombinations(), 146),
    ICEBERG_OUTSIDE_OF_ADVERTISED_LIMIT(0, newCombinations(), 149),
    ICE_BOOM(0, newCombinations(), 147),
    ICE_CONTROL_ZONE(0, newCombinations(), 148),
    JAMMING_EXERCISE(0, newCombinations(), 150),
    LEADING_RANGE_LIGHTS_OUT_OF_SYNCHRONIZATION(0, createCombinations(newCombination().withIncident(78).withNatures(1, 2)), 133),
    LIGHTED_BEACON_LIGHT_DAMAGED(0, createCombinations(newCombination().withIncident(66).withNatures(1, 2)), 173),
    LIGHTED_BEACON_LIGHT_NOT_SYNCHRONIZED(0, createCombinations(newCombination().withIncident(65).withNatures(1, 2)), 174),
    LIGHTED_BEACON_LIGHT_UNLIT(0, createCombinations(newCombination().withIncident(63).withNatures(1, 2)), 175),
    LIGHTED_BEACON_LIGHT_UNRELIABLE(0, createCombinations(newCombination().withIncident(64).withNatures(1, 2)), 176),
    LIGHT_BUOY_LIGHT_DAMAGED(0, createCombinations(newCombination().withIncident(46).withNatures(1, 2)), 151),
    LIGHT_BUOY_LIGHT_NOT_SYNCHRONIZED(0, createCombinations(newCombination().withIncident(45).withNatures(1, 2)), 152),
    LIGHT_BUOY_LIGHT_UNLIT(0, createCombinations(newCombination().withIncident(43).withNatures(1, 2)), 153),
    LIGHT_BUOY_LIGHT_UNRELIABLE(0, createCombinations(newCombination().withIncident(44).withNatures(1, 2)), 154),
    LIGHT_CHANGE(15, newCombinations(), 155),
    LIGHT_DAYMARK_UNRELIABLE(0, createCombinations(newCombination().withIncident(60).withNatures(1, 2)), 156),
    LIGHT_ESTABLISHMENT(4, newCombinations(), 157),
    LIGHT_OPERATING_PROPERLY(0, createCombinations(newCombination().withIncidents(55, 56, 57, 58, 59, 60, 78, 102, 108, 88, 95).withNature(3)), 158),
    LIGHT_OUT_OF_SYNCHRONIZATION(0, createCombinations(newCombination().withIncident(59).withNatures(1, 2)), 159),
    LIGHT_RANGE_REDUCED(0, createCombinations(newCombination().withIncident(57).withNatures(1, 2)), 160),
    LIGHT_REMOVAL(29, newCombinations(), 162),
    LIGHT_RE_ESTABLISHMENT(0, newCombinations(), 161),
    LIGHT_SPAR_BUOY_LIGHT_DAMAGED(0, newCombinations(), 163),
    LIGHT_SPAR_BUOY_LIGHT_NOT_SYNCHRONIZED(0, newCombinations(), 164),
    LIGHT_SPAR_BUOY_LIGHT_UNLIT(0, newCombinations(), 165),
    LIGHT_SPAR_BUOY_LIGHT_UNRELIABLE(0, newCombinations(), 166),
    LIGHT_TEMPORARY_CHANGE(16, createCombinations(newCombination().withIncident(88).withNatures(1, 2)), 167),
    LIGHT_TEMPORARY_ESTABLISHMENT(46, newCombinations(), 168),
    LIGHT_TEMPORARY_REMOVAL(30, createCombinations(newCombination().withIncident(95).withNatures(1, 2)), 169),
    LIGHT_UNLIT(0, createCombinations(newCombination().withIncident(55).withNatures(1, 2)), 170),
    LIGHT_UNRELIABLE(0, createCombinations(newCombination().withIncident(56).withNatures(1, 2)), 171),
    LIGHT_WITHOUT_RHYTHM(0, createCombinations(newCombination().withIncident(58).withNatures(1, 2)), 172),
    LOCAL_HEALTH_AUTHORITY_NOTICE(0, newCombinations(), 177),
    LOCK_CLOSED(0, newCombinations(), 178),
    LOG_ADRIFT(0, newCombinations(), 179),
    LOG_BOOM_ADRIFT(0, newCombinations(), 180),
    LONG_FISHING_GEAR(0, newCombinations(), 204),
    LORAN_C_OPERATING_PROPERLY(0, newCombinations(), 181),
    LORAN_C_OUT_OF_SERVICE(0, newCombinations(), 182),
    LORAN_C_STATION_REMOVAL(0, newCombinations(), 184),
    LORAN_C_STATION_TEMPORARY_REMOVAL(0, newCombinations(), 185),
    LORAN_C_UNREALIABLE(0, newCombinations(), 183),
    LOW_WATER_LEVEL(0, newCombinations(), 186),
    MARINE_AIDS_TO_NAVIGATION_UNRELIABLE(0, newCombinations(), 187),
    MARINE_MAMMALS(0, newCombinations(), 205),
    MARITIME_SECURITY_LEVEL_CHANGE(0, newCombinations(), 188),
    MF_SERVICE(0, newCombinations(), 189),
    MILITARY_EXERCISE(0, newCombinations(), 190),
    MILITARY_OPERATION(0, newCombinations(), 191),
    MINES(0, newCombinations(), 206),
    MSI_SERVICES(0, newCombinations(), 192),
    NATIONAL_HEALTH_AUTHORITY_NOTICE(0, newCombinations(), 193),
    NAVTEX(0, newCombinations(), 194),
    NUMEROUS_FISHING_VESSELS(0, newCombinations(), 196),
    OBJECT_ADRIFT(0, newCombinations(), 197),
    OFFSHORE_RIGS_PLATFORM_CHG(0, newCombinations(), 198),
    OPEN_CLOSE_HARBOUR(0, newCombinations(), 199),
    OPEN_CLOSE_SWING_BRIDGE(0, newCombinations(), 200),
    OPEN_CLOSE_WATERWAY(0, newCombinations(), 201),
    OPERATION_ON_CABLE(0, newCombinations(), 65),
    OPERATION_ON_PIPE(0, newCombinations(), 203),
    PIPE_LAYING_OPERATION(0, newCombinations(), 202),
    RACON_CHANGE(25, newCombinations(), 209),
    RACON_ESTABLISHMENT(10, newCombinations(), 210),
    RACON_OPERATING_PROPERLY(0, createCombinations(newCombination().withIncidents(85, 105, 93, 101).withNature(3)), 211),
    RACON_OUT_OF_SERVICE(0, newCombinations(), 212),
    RACON_REMOVAL(41, newCombinations(), 213),
    RACON_TEMPORARY_CHANGE(26, createCombinations(newCombination().withIncident(93).withNatures(1, 2)), 214),
    RACON_TEMPORARY_ESTABLISHMENT(52, newCombinations(), 215),
    RACON_TEMPORARY_REMOVAL(42, createCombinations(newCombination().withIncident(101).withNatures(1, 2)), 216),
    RACON_UNREALIABLE(0, createCombinations(newCombination().withIncident(105).withNatures(1, 2)), 217),
    RADAR_SURVEILLANCE_SYSTEM(0, newCombinations(), 218),
    RAMARKREMOVAL(0, newCombinations(), 223),
    RAMARK_CHANGE(0, newCombinations(), 219),
    RAMARK_ESTABLISHMENT(0, newCombinations(), 220),
    RAMARK_OPERATING_PROPERLY(0, newCombinations(), 221),
    RAMARK_OUT_OF_SERVICE(0, newCombinations(), 222),
    RAMARK_TEMPORARY_CHANGE(0, newCombinations(), 224),
    RAMARK_TEMPORARY_ESTABLISHMENT(0, newCombinations(), 225),
    RAMARK_TEMPORARY_REMOVAL(0, newCombinations(), 226),
    RAMARK_UNREALIABLE(0, newCombinations(), 227),
    REAR_LEADING_RANGE_BEACON_RESTORED_TO_NORMAL(0, createCombinations(newCombination().withIncident(80).withNature(3)), 228),
    REAR_LEADING_RANGE_BEACON_UNRELIABLE(0, createCombinations(newCombination().withIncident(80).withNatures(1, 2)), 229),
    REAR_LEADING_RANGE_LIGHT_IS_OPERATING_PROPERLY(0, createCombinations(newCombination().withIncidents(71, 73, 75, 77).withNature(3)), 230),
    REAR_LEADING_RANGE_LIGHT_LIGHT_RANGE_REDUCED(0, createCombinations(newCombination().withIncident(75).withNatures(1, 2)), 231),
    REAR_LEADING_RANGE_LIGHT_LIGHT_UNLIT(0, createCombinations(newCombination().withIncident(71).withNatures(1, 2)), 232),
    REAR_LEADING_RANGE_LIGHT_UNRELIABLE(0, createCombinations(newCombination().withIncident(73).withNatures(1, 2)), 233),
    REAR_LEADING_RANGE_LIGHT_WITHOUT_RHYTHM(0, createCombinations(newCombination().withIncident(77).withNatures(1, 2)), 234),
    REGATTA_RACE(0, newCombinations(), 235),
    REGULATION(0, newCombinations(), 195),
    RENEWABLE_ENERGY_DEVICE_FARM(0, newCombinations(), 236),
    RESTRICTED_AREA(0, newCombinations(), 237),
    SANDSPIT(0, newCombinations(), 238),
    SAR_EXERCISE(0, newCombinations(), 239),
    SAR_OPERATION(0, newCombinations(), 240),
    SCIENTIFIC_BUOY_ADRIFT(0, newCombinations(), 241),
    SCIENTIFIC_BUOY_APPARATUS(0, newCombinations(), 208),
    SCIENTIFIC_MOORING(0, newCombinations(), 242),
    SCIENTIFIC_SURVEY(0, newCombinations(), 243),
    SEAPLANE_OPERATIONS(0, newCombinations(), 245),
    SEASONAL_DECOMMISSIONING_COMPLETE(0, newCombinations(), 248),
    SEASONAL_DECOMMISSIONING_IN_PROGRESS(0, newCombinations(), 249),
    SEASONAL_RECOMMISSIONING_COMPLETE(0, newCombinations(), 246),
    SEASONAL_RECOMMISSIONING_IN_PROGRESS(0, newCombinations(), 247),
    SEA_TRIALS(0, newCombinations(), 244),
    SECTOR_LIGHT_CHANGE(0, newCombinations(), 251),
    SECTOR_LIGHT_SECTOR_OBSCURED(0, createCombinations(newCombination().withIncident(102).withNatures(1, 2)), 250),
    SECTOR_LIGHT_TEMPORARY_CHANGE(0, createCombinations(newCombination().withIncident(108).withNatures(1, 2)), 252),
    SECURITY_REGULATION_CHANGE(0, newCombinations(), 253),
    SEISMIC_SURVEY(0, newCombinations(), 254),
    SHALLOW_DEPTH_REPORTED(0, newCombinations(), 256),
    SHALLOW_DEPTH_CONFIRMED(0, newCombinations(), 255),
    SPAR_BUOY_ADRIFT(0, newCombinations(), 257),
    SPAR_BUOY_DAMAGED(0, newCombinations(), 258),
    SPAR_BUOY_DESTROYED(0, newCombinations(), 259),
    SPAR_BUOY_MISSING(0, newCombinations(), 260),
    SPAR_BUOY_MOVE(0, newCombinations(), 261),
    SPAR_BUOY_OFF_POSITION(0, newCombinations(), 262),
    SPAR_BUOY_RE_ESTABLISHMENT(0, newCombinations(), 263),
    SPAR_BUOY_RESTORED_TO_NORMAL(0, newCombinations(), 264),
    SPAR_BUOY_TOPMARK_MISSING(0, newCombinations(), 265),
    SPAR_BUOY_WITHDRAWN(0, newCombinations(), 266),
    STORM_SURGE(0, newCombinations(), 267),
    SUBMARINE_CABLE_CHG(0, newCombinations(), 268),
    SUBMARINE_PIPELINE_CHG(0, newCombinations(), 269),
    SUBMERGED_FISH_NET(0, newCombinations(), 207),
    SUBMERGED_OBJECT(0, newCombinations(), 270),
    SUBSURFACE_MOORING(0, newCombinations(), 271),
    SWIMMERS(0, newCombinations(), 272),
    TEMPORARY_BUOY_BUOYS(0, createCombinations(newCombination().withIncident(42).withNatures(1, 2)), 273),
    TIDE_GAUGE(0, newCombinations(), 274),
    TRAFFIC_CONGESTION(0, newCombinations(), 275),
    TSUNAMI_WARNING(0, newCombinations(), 276),
    UNCHARTED_ROCK(0, newCombinations(), 277),
    UNDERWATER_OPERATIONS(0, newCombinations(), 278),
    UNIDENTIFIED_RADAR_TARGET_POSSIBLE_ICEBERG(0, newCombinations(), 279),
    UNWIELDY_TOW(0, newCombinations(), 280),
    VERTICAL_CLEARANCE_REDUCED(0, newCombinations(), 290),
    VESSEL_ADRIFT(0, newCombinations(), 291),
    VESSEL_DISABLED(0, newCombinations(), 292),
    VHF_SERVICE(0, newCombinations(), 293),
    VOLCANO_ACTIVITY(0, newCombinations(), 294),
    VTS_CHANGE(0, newCombinations(), 295),
    V_AIS_CHANGE(23, newCombinations(), 281),
    V_AIS_ESTABLISHMENT(9, newCombinations(), 282),
    V_AIS_OPERATING_PROPERLY(0, createCombinations(newCombination().withIncidents(84, 104, 92, 100).withNature(3)), 283),
    V_AIS_OUT_OF_SERVICE(0, createCombinations(newCombination().withIncident(84).withNatures(1, 2)), 284),
    V_AIS_REMOVAL(39, newCombinations(), 285),
    V_AIS_TEMPORARY_CHANGE(24, createCombinations(newCombination().withIncident(92).withNatures(1, 2)), 286),
    V_AIS_TEMPORARY_ESTABLISHMENT(51, newCombinations(), 287),
    V_AIS_TEMPORARY_REMOVAL(40, createCombinations(newCombination().withIncident(100).withNatures(1, 2)), 288),
    V_AIS_UNREALIABLE(0, createCombinations(newCombination().withIncident(104).withNatures(1, 2)), 289),
    WATERWAY_RECOMMENDED_NOT_RECOMMENDED_FOR_SHIPPING(0, newCombinations(), 296),
    WHARF_CONSTRUCTION(0, newCombinations(), 297),
    WORKS_IN_PROGRESS(0, newCombinations(), 298),
    WORLD_HEALTH_ORGANIZATION_NOTICE(0, newCombinations(), 299);

    private final int[] cmsCode;
    private final NatureIntituleCombinations combinations;
    private final int s124Code;

    WarningHazardTypeDetails(int cmsCode, NatureIntituleCombinations combinations, int s124Code) {
        this.cmsCode = new int[]{cmsCode};
        this.combinations = combinations;
        this.s124Code = s124Code;
    }

    @Nullable
    public static WarningHazardTypeDetails fromCodes(int natureCode, int cmsCode, int incidentCode) {
        for (WarningHazardTypeDetails at : WarningHazardTypeDetails.values()) {
            if (Arrays.stream(at.getCmsCode()).boxed().anyMatch(code -> code == cmsCode) && at.getCombinations().hasNatureAndIncident(natureCode,
                incidentCode)) {
                return at;
            }
        }
        return null;
    }

    private static NatureIntituleCombinations newCombinations() {
        return NatureIntituleCombinations.builder().build();
    }

    private static NatureIntituleCombinations createCombinations(NatureIntituleCombination... combinationList) {
        return newCombinations().withCombinations(combinationList);
    }

    private static NatureIntituleCombination newCombination() {
        return NatureIntituleCombination.builder().build();
    }

    @Data
    @Builder
    @AllArgsConstructor
    static class NatureIntituleCombinations {
        @Builder.Default
        private List<NatureIntituleCombination> combinations = new ArrayList<>();

        public NatureIntituleCombinations withCombinations(NatureIntituleCombination... combinationList) {
            combinations.addAll(List.of(combinationList));
            return this;
        }

        public boolean hasNatureAndIncident(int natureCode, int incidentCode) {
            final Predicate<Integer> predicateIncidentCode = code -> code == incidentCode;
            final Predicate<Integer> predicateNatureCode = code -> code == natureCode;

            final Predicate<NatureIntituleCombination> predicateCombination =
                combination -> (combination.incidentCodes.stream().anyMatch(predicateIncidentCode) || isIncidentCodeOutOfBoundsAndHasOnlyZeroAsIncidentCode(incidentCode, combination.incidentCodes))
                && combination.natureCodes.stream().anyMatch(predicateNatureCode);
            return (combinations.isEmpty() && incidentCode == 0 && natureCode == 0) || combinations.stream().anyMatch(predicateCombination);
        }

        private static boolean isIncidentCodeOutOfBoundsAndHasOnlyZeroAsIncidentCode(int incidentCode, List<Integer> incidentCodes) {
            return isIncidentCodeOutOfBounds(incidentCode) && supportsOutOfBoundsIncidentCode(incidentCodes);
        }

        // check if incident code is out of bounds specified in SISSY spec (cf ATON_OPERATING_PROPERLY)
        private static boolean isIncidentCodeOutOfBounds(int incidentCode) {
            return (incidentCode > 180 || incidentCode < 42) && incidentCode > 0;
        }

        // case when .withIncident(0) to support other incident codes than range [42, 180] (cf ATON_OPERATING_PROPERLY)
        private static boolean supportsOutOfBoundsIncidentCode(List<Integer> incidentCodes) {
            return incidentCodes.size() == 1 && incidentCodes.getFirst() == 0;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    static class NatureIntituleCombination {
        @Builder.Default
        private List<Integer> natureCodes = new ArrayList<>();

        @Builder.Default
        private List<Integer> incidentCodes = new ArrayList<>();

        public NatureIntituleCombination withNature(Integer nature) {
            natureCodes.add(nature);
            return this;
        }

        public NatureIntituleCombination withNatures(Integer... natures) {
            natureCodes.addAll(List.of(natures));
            return this;
        }

        public NatureIntituleCombination withIncident(Integer incident) {
            incidentCodes.add(incident);
            return this;
        }

        public NatureIntituleCombination withIncidents(Integer... incidents) {
            incidentCodes.addAll(List.of(incidents));
            return this;
        }
    }
}
