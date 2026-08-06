# Current Implementation

## Base Revision

Public_Brproject

## Target

L2J ExtMods Interlude

## Status

Character Killing Monuments funcionando.

## Features

- Top PvP das últimas 24 horas
- Top PK das últimas 24 horas
- Fake Player Polymorph
- Monumentos dinâmicos
- Atualização automática por ciclo
- HTML personalizado
- SQL próprio

## Integrated Files

### Java

- CharacterKillingManager.java
- L2PcPolymorph.java
- L2TopPKMonumentInstance.java
- L2TopPvPMonumentInstance.java

### Core Modifications

- Config.java
- GameServer.kt
- AbstractNpcInfo.java

### Data

- CharacterKillingMonuments.properties
- TopMonument.xml
- HTMLs

### SQL

- character_kills_info.sql
- character_kills_snapshot.sql
