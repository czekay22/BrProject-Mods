# 🏗️ Character Killing Monuments BR Edition - Architecture

> Technical overview of the project structure.

> Visão técnica da estrutura do projeto.

---

# 📖 Overview

## English

Character Killing Monuments BR Edition is composed of multiple systems working together to display and update PvP and PK ranking monuments inside Lineage 2.

The main components are:

- Ranking system.
- Character data loading.
- Monument instances.
- Player appearance synchronization.
- Network packet communication.

## Português

O Character Killing Monuments BR Edition é composto por diversos sistemas trabalhando juntos para exibir e atualizar monumentos de ranking PvP e PK dentro do Lineage 2.

Os principais componentes são:

- Sistema de ranking.
- Carregamento de dados dos personagens.
- Instâncias dos monumentos.
- Sincronização da aparência do jogador.
- Comunicação através de packets.

---

# 🧩 High Level Flow
Player PvP / PK Activity
|
v
CharacterKillingManager
|
v
Ranking Calculation
|
v
Winner Selection
|
v
Monument Update
|
v
Player Appearance Synchronization
|
v
Client Visualization

---

# 🏆 Ranking System

Responsible for:

- Tracking PvP kills.
- Tracking PK kills.
- Selecting top players.
- Providing data for monuments.

Future documentation:

- Database structure.
- Ranking queries.
- Update process.

---

# 🗿 Monument System

Responsible for:

- Creating monument NPC instances.
- Updating monument information.
- Displaying winners.

Main components:
L2TopPKMonumentInstance
|
v
L2PcPolymorph
|
v
NPC Representation

---

# 🎭 Player Appearance System

The monument system uses player appearance synchronization.

Main concepts:

- Fake Player.
- Polymorph system.
- NPC packet adaptation.

Related components:
Player Data
|
v
Polymorph Data
|
v
NpcInfoPolymorph
|
v
Client Rendering

---

# 📡 Packet Communication

The client does not receive a real player object.

The server must create NPC information packets that simulate player appearance.

Important areas:

- NPC information packets.
- Equipment visualization.
- Appearance data.
- Client compatibility.

---

# ⚙️ Configuration

Configuration will be integrated with the server configuration system.

Future documentation:

- Enable/disable options.
- Ranking settings.
- Update intervals.

---

# 🗄️ Database

The project may require database changes.

Documentation will include:

- Tables.
- Queries.
- Installation scripts.
- Update scripts.

---

# 🔄 Development Philosophy

Architecture decisions should prioritize:

- Compatibility.
- Maintainability.
- Clear organization.
- Minimal impact on the core.

---

# 📌 Future Updates

This document will evolve together with the implementation.

Every important architectural change should be documented here.

---

## Player Snapshot System

The monument does not store a live player instance.

The system stores the character ID and reconstructs a player representation through L2PcPolymorph.loadMonumentPlayer().

Flow:

Database
-> Character ID
-> Monument Player Loader
-> Fake Player Representation
-> NPC Polymorph
-> Client Packet