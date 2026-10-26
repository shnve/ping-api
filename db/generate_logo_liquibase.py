import csv
import uuid

xml_template = """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" logicalFilePath="db.data-shn-logos-binary.xml"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

    <changeSet id="DB.Data.Insert-SHN-Logos" author="antigravity" context="local,qlf,prod">
        <sql stripComments="true">
{sql_statements}
        </sql>
    </changeSet>

</databaseChangeLog>
"""

sql_statements = []

with open('C:\\Users\\meben\\Documents\\SHN\\shnve_s124\\ping-api\\db\\logos.csv', 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    for row in reader:
        logo_key = row['logo_key']
        description = row['description']
        height_with_unit = row['height_with_unit']
        content_b64 = row['content_b64'].replace('\n', '')
        alt_txt = row['alt_txt'].replace("'", "''")
        alt_txt_international = row['alt_txt_international'].replace("'", "''")
        
        sql = f"""
            INSERT INTO static_logo (id, logo_key, description, height_with_unit, content, alt_txt, alt_txt_international, locked)
            VALUES ('{uuid.uuid4()}', '{logo_key}', '{description}', '{height_with_unit}', decode('{content_b64}', 'base64'), '{alt_txt}', '{alt_txt_international}', false)
            ON CONFLICT (logo_key, description) DO UPDATE SET 
                content = EXCLUDED.content,
                height_with_unit = EXCLUDED.height_with_unit,
                alt_txt = EXCLUDED.alt_txt,
                alt_txt_international = EXCLUDED.alt_txt_international;
        """
        sql_statements.append(sql)

final_xml = xml_template.format(sql_statements="\n".join(sql_statements))

with open('C:\\Users\\meben\\Documents\\SHN\\shnve_s124\\ping-api\\src\\main\\resources\\db\\data\\db.data-shn-logos-binary.xml', 'w', encoding='utf-8') as f:
    f.write(final_xml)

print("Liquibase file generated successfully.")
