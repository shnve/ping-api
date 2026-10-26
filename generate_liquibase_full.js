const fs = require('fs');

const esData = JSON.parse(fs.readFileSync('C:/Users/meben/Documents/SHN/shnve_s124/ping-web/src/assets/i18n/es.json', 'utf8'));

const detailsDict = esData.enum["warning-hazard-type-details"];
const generalDict = esData.enum["warning-hazard-type-general"];
const typeDict = esData.enum["warning-part-type-expression"]; // If this is warning_type

const xmlHeader = `<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.4.xsd">

    <changeSet id="DB.Data.Warning_Hazard_Types.Spanish.V2" author="antigravity">
`;

const xmlFooter = `    </changeSet>
</databaseChangeLog>
`;

let xmlContent = xmlHeader;
let updateCount = 0;

function addUpdates(dict, tableName) {
    if (!dict) return;
    for (const [keyName, translationRaw] of Object.entries(dict)) {
        // Convert 'light-buoy-light-unreliable' to 'LIGHT_BUOY_LIGHT_UNRELIABLE'
        const enumName = keyName.toUpperCase().replace(/-/g, '_');
        let translation = translationRaw.replace(/'/g, "''");
        xmlContent += `        <update tableName="${tableName}">\n`;
        xmlContent += `            <column name="translation" value="${translation}"/>\n`;
        xmlContent += `            <where>enum_name = '${enumName}'</where>\n`;
        xmlContent += `        </update>\n`;
        updateCount++;
    }
}

addUpdates(detailsDict, 'warning_hazard_type_details');
addUpdates(generalDict, 'warning_hazard_type_general');
// For warning_type, check if keys exist
if (typeDict) {
    addUpdates(typeDict, 'warning_type');
}

xmlContent += xmlFooter;

fs.writeFileSync('C:/Users/meben/Documents/SHN/shnve_s124/ping-api/src/main/resources/db/data/db.data-shn-s124-spanish-v2.xml', xmlContent, 'utf8');
console.log("Generated db.data-shn-s124-spanish-v2.xml successfully!");
console.log("Total updates:", updateCount);
