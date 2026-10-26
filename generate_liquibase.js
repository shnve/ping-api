const fs = require('fs');

const keysMap = {
  "CONTAINER_ADRIFT": "container-adrift",
  "SCIENTIFIC_BUOY_ADRIFT": "scientific-buoy-adrift",
  "VESSEL_ADRIFT": "vessel-adrift",
  "DERELICT_VESSEL_ADRIFT": "derelict-vessel-adrift",
  "DEAD_WHALE_ADRIFT": "dead-whale-adrift",
  "LOG_ADRIFT": "log-adrift",
  "DEAD_HEAD_ADRIFT": "dead-head-adrift",
  "LOG_BOOM_ADRIFT": "log-boom-adrift",
  "DOCK_ADRIFT": "dock-adrift",
  "FISH_NET_ADRIFT": "fish-net-adrift",
  "FLOATING_DEBRIS": "floating-debris",
  "OBJECT_ADRIFT": "object-adrift",
  "SANDSPIT": "sandspit",
  "SHALLOW_DEPTH_REPORTED": "shallow-depth-reported",
  "SHALLOW_DEPTH_CONFIRMED": "shallow-depth-confirmed",
  "SUBMERGED_FISH_NET": "submerged-fish-net",
  "UNCHARTED_ROCK": "uncharted-rock",
  "DANGEROUS_WRECK": "dangerous-wreck",
  "SUBSURFACE_MOORING": "subsurface-mooring",
  "SUBMERGED_OBJECT": "submerged-object",
  "ACOUSTIC_RECORDER": "acoustic-recorder",
  "SCIENTIFIC_BUOY_APPARATUS": "scientific-buoy-apparatus",
  "SCIENTIFIC_MOORING": "scientific-mooring",
  "TIDE_GAUGE": "tide-gauge",
  "OPEN_CLOSE_WATERWAY": "open-close-waterway",
  "WATERWAY_RECOMMENDED_NOT_RECOMMENDED_FOR_SHIPPING": "waterway-recommended-not-recommended-for-shipping",
  "VTS_CHANGE": "vts-change",
  "RADAR_SURVEILLANCE_SYSTEM": "radar-surveillance-system",
  "REGULATION": "regulation",
  "RESTRICTED_AREA": "restricted-area",
  "EXCLUSION_ZONES": "exclusion-zones",
  "REGATTA_RACE": "regatta-race",
  "SWIMMERS": "swimmers",
  "MARINE_MAMMALS": "marine-mammals",
  "FIREWORKS": "fireworks",
  "CLUSTER_OF_FISHING_VESSELS": "cluster-of-fishing-vessels",
  "TRAFFIC_CONGESTION": "traffic-congestion",
  "VESSEL_DISABLED": "vessel-disabled",
  "OPEN_CLOSE_HARBOUR": "open-close-harbour",
  "OPEN_CLOSE_SWING_BRIDGE": "open-close-swing-bridge",
  "VERTICAL_CLEARANCE_REDUCED": "vertical-clearance-reduced",
  "DREDGING_OPERATION": "dredging-operation",
  "BREAKWATER_CONSTRUCTION": "breakwater-construction",
  "WHARF_CONSTRUCTION": "wharf-construction",
  "DIVING_OPERATIONS": "diving-operations",
  "CABLE_LAYING_OPERATION": "cable-laying-operation",
  "OPERATION_ON_CABLE": "operation-on-cable",
  "PIPE_LAYING_OPERATION": "pipe-laying-operation",
  "OPERATION_ON_PIPE": "operation-on-pipe",
  "WORKS_IN_PROGRESS": "works-in-progress",
  "SEA_TRIALS": "sea-trials",
  "SEAPLANE_OPERATIONS": "seaplane-operations",
  "MILITARY_EXERCISE": "military-exercise",
  "FIRING_EXERCISE": "firing-exercise",
  "JAMMING_EXERCISE": "jamming-exercise",
  "MILITARY_OPERATION": "military-operation",
  "BLASTING_OPERATION": "blasting-operation",
  "ANTI_POLLUTION_EXERCISE": "anti-pollution-exercise",
  "ANTI_POLLUTION_OPERATION": "anti-pollution-operation",
  "SAR_EXERCISE": "sar-exercise",
  "SAR_OPERATION": "sar-operation"
};

const esData = JSON.parse(fs.readFileSync('C:/Users/meben/Documents/SHN/shnve_s124/ping-web/src/assets/i18n/es.json', 'utf8'));
const detailsDict = esData.enum["warning-hazard-type-details"];

const xmlHeader = `<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.4.xsd">

    <changeSet id="DB.Data.Warning_Hazard_Type_Details.Spanish" author="antigravity">
`;

const xmlFooter = `    </changeSet>
</databaseChangeLog>
`;

let xmlContent = xmlHeader;

for (const [enumName, keyName] of Object.entries(keysMap)) {
    let translation = detailsDict[keyName];
    if (translation) {
        translation = translation.replace(/'/g, "''");
        xmlContent += `        <update tableName="warning_hazard_type_details">\n`;
        xmlContent += `            <column name="translation" value="${translation}"/>\n`;
        xmlContent += `            <where>enum_name = '${enumName}'</where>\n`;
        xmlContent += `        </update>\n`;
    }
}

xmlContent += xmlFooter;

fs.writeFileSync('C:/Users/meben/Documents/SHN/shnve_s124/ping-api/src/main/resources/db/data/db.data-shn-s124-spanish.xml', xmlContent, 'utf8');
console.log("Generated db.data-shn-s124-spanish.xml successfully!");
