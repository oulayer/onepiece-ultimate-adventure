# One Piece: Ultimate Adventure — Forge 1.20.1

Mod de ação/RPG de One Piece para **Minecraft 1.20.1 / Forge 47.4.x** (Java 17), desenvolvido em etapas conforme a metodologia do projeto: cada etapa é compilada, testada e validada antes de avançar.

## Estado atual (etapas 1–6 concluídas + base das etapas 7, 9, 10)

| Etapa | Status |
|---|---|
| 1. Análise de versão/plataforma | ✅ Forge 47.4.29, mapeamentos oficiais, Java 17 |
| 2. Arquitetura e estrutura | ✅ pacotes independentes (ver abaixo) |
| 3. Combate básico | ✅ combos, aéreo, pesado carregado, dash/esquiva perfeita, bloqueio, parry, contra-ataque, finalização |
| 4. Sistema de habilidades | ✅ INSTANT / CHARGE / HOLD / TOGGLE, cooldown, energia, maestria, variantes por transformação |
| 5. Fruta completa de demonstração | ✅ **Gomu Gomu no Mi** (7 técnicas + 3 variantes Jet + passivas) |
| 6. Haki | ✅ Busoshoku, Kenbunshoku e Haoshoku com níveis 1–10, versões avançadas (Emissão, Visão do Futuro, Infusão do Rei) |
| 7. Animações e transformações | ✅ framework de transformações, Gear 2/4 (Boundman, Snakeman)/5, Zoan Fênix (híbrida e completa), 80 animações com 1ª pessoa |
| 8. Chefes | ⏳ |
| 9. Destruição e efeitos | 🟡 crateras, rachaduras, ondas de choque, túneis, árvores derrubadas, detritos, restauração opcional |
| 10. HUD, progressão, multiplayer | 🟡 HUD de habilidades, ficha do personagem, níveis/atributos/maestria, sincronização cliente-servidor |

## Como compilar e testar

```bash
./gradlew build                 # gera build/libs/onepiece-0.1.0.jar (reobfuscado, pronto para a pasta mods)
./gradlew runGameTestServer     # 22 testes funcionais em servidor headless real
./gradlew runClient             # cliente de desenvolvimento
./gradlew runClient -Pautotest  # roteiro visual automático: cria mundo plano, usa as técnicas e salva screenshots em run/screenshots
```

Os GameTests cobrem (Haki): despertar por nível/potencial e treino, travas (gauge de haki, níveis, "requer ativo"), Armamento vencendo a borracha com bônus correto, Visão do Futuro esquivando 5 ataques seguidos, Emissão atravessando o bloqueio, julgamento de vontade do Haoshoku (galinha desmaia, golem fica intimidado, warden resiste) e o caminho completo carregar → liberar. Também cobrem: persistência dos dados, travas de habilidade (maestria/cooldown/energia), troca para variantes Jet no Gear Second, o Pistol acertando um alvo de verdade e concedendo maestria, encadeamento de combo, parry (anula + atordoa + abre contra-ataque), bloqueio (75%, só frontal), corpo de borracha (contundente/corte/raio), água do mar, regras de proteção da cratera (baú, bedrock, obsidiana) e o orçamento de blocos por tick.

## Controles (todos reconfiguráveis em Opções → Controles)

| Tecla | Ação |
|---|---|
| `G` | Liga/desliga o **modo de combate** (clique esquerdo = combo leve, direito = ataque pesado carregável, apenas com mão vazia ou lâmina) |
| `Z X C V B N` | Técnicas 1–6 da fruta (segurar para técnicas de carga/canalização) |
| `M` (segurar) | Roda de transformações: aponte e solte para ligar/desligar uma forma |
| `Espaço` no ar | Mecânica da forma ativa (quique, dash aéreo, bater asas) |
| `Alt esquerdo` | Dash direcional / esquiva (i-frames; esquivar no tempo certo = **esquiva perfeita**) |
| `R` (segurar) | Bloquear; bloquear logo antes do golpe = **parry** |
| `K` | Ficha do personagem (atributos, maestria, lista de técnicas, Haki) |
| `H` | Busoshoku Haki (alternar) |
| `J` | Kenbunshoku Haki (alternar) |
| `Y` | Armamento: Emissão (segurar para carregar; requer Busoshoku ativo) |
| `U` | Observação: Visão do Futuro (requer Kenbunshoku ativo) |
| `I` | Haoshoku Haki (segurar para carregar, soltar para liberar) |
| `O` | Infusão do Rei (alternar; requer Busoshoku ativo) |

Pesado no ar = **ataque de queda** com onda de choque e cratera proporcional à altura.

## Gomu Gomu no Mi

| Slot | Técnica | Maestria | Mecânica |
|---|---|---|---|
| 1 | Pistol → *Jet Pistol* | 0 | punho real que estica do ombro até ~22 blocos e volta; arremessa, amassa paredes |
| 2 | Whip | 5 | perna varre 200° em 5 ticks, fatia a fatia; empurra para o lado, corta vegetação |
| 3 | Gatling → *Jet Gatling* | 10 | segurar: rajada de punhos com atordoamento curto que prende o alvo |
| 4 | Rocket | 15 | agarra o bloco mirado e se lança, atropelando inimigos no caminho |
| 5 | Bazooka → *Jet Bazooka* | 22 | carregar: golpe duplo que quebra a defesa, arremessa e explode o terreno |
| 6 | Gear Second | 30 | transformação: +35% dano/velocidade, técnicas Jet, pele vermelha, vapor; cansaço ao final |
| 7 | Gear Third: Gigant Pistol | 45 | infla o braço (escala animada), punho gigante perfura tudo, cava túnel e termina em cratera; encolhe depois |

Passivas: imune a raios, metade do dano contundente (lâminas cortam normalmente), sem dano de queda e quica em quedas altas. Na água do mar perde os poderes, gasta energia e afunda.

## Transformações (etapa 7)
Segure **`M`** para abrir a **roda de transformações**. Aponte o mouse para uma forma e solte a tecla (ou clique) para ligar ou desligar. A forma ativa aparece no último slot da barra, e as formas são exclusivas entre si (só um Gear por vez). Durante a sequência de ativação você não age, mas também não pode ser ferido. **Pular no ar** aciona a mecânica de cada forma.

| Forma | Requisito | Efeito | Técnicas que mudam |
|---|---|---|---|
| Gear Second | maestria 30 | +35% de dano e velocidade, pele vermelha, vapor | Jet Pistol, Jet Gatling, Jet Bazooka |
| Gear Fourth: **Boundman** | maestria 60 + Armamento Nv4 | corpo enorme e revestido, quica sem parar, 3 quiques no ar, +80% de dano, −40% de dano recebido, hitbox maior. **Ao terminar: Haki esgotado por 30 s** | Kong Gun, Culverin, Kong Organ, Rhino Schneider, Leo Bazooka, King Kong Gun |
| Gear Fourth: **Snakeman** | maestria 70 + Armamento Nv4 | mais rápido, dash no ar, socos **teleguiados** e serpenteantes; silhueta própria (magra, cabelo espetado, pontos vermelhos) | Jet Culverin, Hydra, Black Mamba, King Cobra |
| **Gear Fifth** | maestria 100 | branco radiante, tambores da libertação, 5 pulos no ar; tudo que é atingido **vira borracha e quica**, o terreno balança em vez de quebrar. Cabelo de nuvem, anel de nuvens no pescoço, rosto de desenho que gargalha. Deixa o corpo exausto | Mogura Pistol, Gigant, Dawn Gatling, Dawn Rocket, Kaminari, Bajrang Gun |
| Fênix **Híbrida** | maestria 15 | asas de chama azul, plana ao cair, bate as asas no ar | — |
| Fênix **Completa** | maestria 30 | modelo de fênix próprio, **voo real**, hitbox menor | — |

**Tori Tori no Mi, Modelo Fênix** (Zoan mítica): Garra da Fênix, Onda de Chama Azul, Chamas da Regeneração (cura você e aliados) e Marca da Fênix (mergulho do alto). Passivo: as chamas regeneram a maior parte do dano recebido pagando energia, **exceto dano de quem usa Busoshoku**.

### Correções e aprimoramentos (revisão das formas)
- **Gear 4**: revestimento preto com bordas e brilho vermelhos; tatuagens de chama vermelhas (Boundman) e pontos vermelhos (Snakeman); quique contínuo com compressão/expansão; animação de ativação própria do Snakeman.
- **Gear 5**: **Gigant** aumenta a hitbox real 2,6× (alcance +3, passos geram ondas de choque) com crescimento elástico; câmera de 3ª pessoa afasta automaticamente; pose cartunesca no ar.
- **Kaminari** (carregue): raios visuais + dano/paralisia em área, cratera e rachaduras. **Mogura Pistol**: o punho cava e irrompe sob o alvo.
- **Fênix completa**: modelo novo (pescoço, crista, penas primárias, cauda longa com plumas articuladas), estados de voo (subida com batidas rápidas, cruzeiro lento, planar/mergulho com asas travadas, asas dobradas no chão), inclinação do corpo pela trajetória, transição de crescimento ao transformar e rastros de vento. Golpe de garra com animação própria. Nova textura da fruta (abacaxi de chamas azuis com coroa dourada) e modelo de item que faltava.
- Ombros ficam presos ao tronco quando a animação gira o corpo (braços "soltos" em 3ª pessoa corrigidos); espadas seguem o braço animado em 1ª pessoa.
- Testes automáticos: 26 (Hydra multi-alvo, Rhino Schneider, Kaminari em área + paralisia, Gigant cresce e volta).
- Roteiro visual: `./gradlew runClient -Pautotest=moves`.

## Primeira pessoa e menu
- **Primeira pessoa:** com a mão vazia e o modo de combate (ou um poder) ativo, os dois braços aparecem e seguem as mesmas animações do corpo: guarda, jabs alternados, gancho, bloqueio cruzado, cargas, Bazooka, Gigant inflado e Haki. A pausa de impacto e as transições funcionam igual. Os revestimentos de Haki e do Gear Second também aparecem nos braços. Segurando um item, o jogo usa a renderização vanilla.
- **Menu (`K`):** três abas (Status, Akuma no Mi e Haki), com prévia 3D do personagem que segue o mouse, cartões de atributos (shift-clique dá +5), atributos derivados, cartões de técnica com tooltip e cartões de Haki com níveis.
- **Visual do Busoshoku:** na ativação, os braços cruzam na frente do rosto e chamas violeta animadas envolvem os membros. Depois eles endurecem num preto brilhante, com reflexos emissivos e uma borda violeta tremulando.

## Haki

O Haki usa um medidor próprio (roxo na HUD), separado da energia, e evolui do nível 1 ao 10 **pelo uso**. O atributo **Domínio de Haki** aumenta o medidor, a regeneração, a potência e a velocidade de treino.

| Tipo | Desperta | Técnicas | Mecânica |
|---|---|---|---|
| **Busoshoku** | nível 5 do personagem | Revestimento (Nv1), **Emissão** (Nv6) | +18%…+45% de dano e −12%…−45% de dano recebido; golpes revestidos ignoram a resistência da borracha e a intangibilidade Logia (gancho pronto para as Logias); cobertura braços → pernas (Nv4) → corpo inteiro (Nv8), também nos braços esticados da Gomu e nas armas; quando dois revestimentos se chocam, o mais forte anula a defesa do outro. A Emissão causa dano `internal` que ignora armadura, escudo e bloqueio, e a força sai pelas costas do alvo destruindo o terreno, ou implode a parede por dentro |
| **Kenbunshoku** | nível 8 | Observação (Nv1), **Visão do Futuro** (Nv5) | contorno de todos os seres vivos em 18–40 blocos **através das paredes** (somente para você), indicadores de direção de quem tem intenção hostil (o servidor detecta mobs que te miram e jogadores apontando para você em combate), esquiva automática com chance de 30%…75% e recarga curta; a Visão do Futuro esquiva **todos** os ataques por 3–7 s |
| **Haoshoku** | nível 20, **somente** com potencial do Rei (sorteado uma vez por jogador, 35% por padrão) | Onda do Rei (Nv1), **Infusão** (Nv5) | carregar faz raios negros e tremores; ao liberar, cada criatura é julgada pela força de vontade (nível/Haki para jogadores, vida/armadura e tag de chefe para mobs): **desmaia**, fica **intimidada** (perde o alvo, foge e causa −40% de dano) ou resiste; a pressão racha o chão e quebra vidro e folhagem. A Infusão faz todo golpe estalar com raios negros, +22%…+40% de dano, empurrão extra e +0,8 de alcance |

Comandos: `/onepiece haki level <armament|observation|conqueror> <0-10>`, `/onepiece haki potential <true|false>`, `/onepiece haki refill`.

## Arquitetura

```
com.onepiece.ultimate
├── ability/       framework genérico: Ability (ciclo de vida), AbilityManager (regras), AbilityRuntime, registro
├── fruit/         DevilFruit (loadout + passivas), itens, regras de comer; uma subpasta por fruta
│   └── gomu/      cada técnica em sua própria classe
├── combat/        CombatManager (máquina de estados), HitDetector (cone/cápsula), CombatHits, DamageCalculator,
│                  LaunchTracker (impacto contra paredes), CombatEvents (bloqueio/parry/esquiva em QUALQUER dano)
├── destruction/   DestructionManager (API), BlockJob/ShockwaveJob com orçamento por tick, DestructionRules
│                  (proteção, dureza x poder, eventos de quebra p/ mods de claim), TerrainRestoreData
├── data/          capability OPPlayerData (persistente / sincronizada / só servidor) + DataSync por categorias sujas
├── haki/          HakiData (níveis/potencial), HakiManager (despertar, treino, imbuimento, esquiva, ameaças,
│                  regeneração), uma classe por técnica (todas estendem o mesmo framework de habilidades)
├── progression/   XP, níveis, atributos como AttributeModifiers reais, maestria e desbloqueios
├── network/       canal + pacotes (cliente nunca decide resultado, só envia intenção)
├── entity/        StretchFistEntity (punho genérico configurável), DebrisEntity (física, limite por dimensão)
├── client/        animation/ (motor de keyframes data-driven), hud/, render/, particle/, input/, screen/
└── gametest/      testes funcionais
```

Princípios: o servidor é autoritativo; efeitos visuais nunca simulam mecânica; nenhum estado global sem limpeza (mapas fracos, expiração e limpeza ao desconectar/parar o servidor).

### Animações (data-driven)
`assets/onepiece/animations/*.json`: keyframes `[tick, x, y, z, easing]` por osso (`head, body, right_arm, left_arm, right_leg, left_leg, root`), rotação/posição/escala, camadas de estado (posturas) e ação (golpes) com cross-fade automático, hit-stop e 14 curvas de easing. Resource packs podem substituir ou adicionar animações.

### Configuração
`serverconfig/onepiece-server.toml`: destruição (liga/desliga, blocos por ataque, blocos por tick, proteger block entities, respeitar mobGriefing, eventos de quebra, detritos, **restauração do terreno**), combate (janelas de combo/parry, redução do bloqueio, i-frames, multiplicador PvP), frutas (segunda fruta mata, fraqueza ao mar), Haki (níveis de despertar, chance do potencial do Rei, multiplicador de treino, se o Haoshoku afeta jogadores) e progressão.
`config/onepiece-client.toml`: posição/escala da HUD, dicas de teclas, contador de combo, intensidade do tremor de câmera, densidade de partículas.

Comando de administração/teste: `/onepiece fruit set onepiece:gomu_gomu`, `mastery set <0-100>`, `level set`, `xp add`, `points add`, `info`, `reset`, `destruction pending`.

## Adicionando uma nova fruta
1. Criar `fruit/<nome>/` com uma classe por técnica estendendo `Ability` (cada uma com mecânica própria).
2. Uma classe `DevilFruit` com `createAbilities()` e as passivas (`modifyIncomingDamage`, `isImmuneTo`, `onFall`, `passiveTick`).
3. Registrar em `DevilFruits.bootstrap()`, criar o item em `ModItems`, animações JSON, ícones e traduções.

## Usando seus próprios sons
Cada efeito tem um evento próprio, e as variações são sorteadas a cada uso. Para trocar por sons seus, crie um resource pack com arquivos `.ogg` (mono) nestes caminhos:

```
meu_pack/pack.mcmeta
meu_pack/assets/onepiece/sounds/gomu/stretch_1.ogg   (… stretch_2, stretch_3)
meu_pack/assets/onepiece/sounds/gomu/snap_1.ogg      (… snap_2, snap_3)
meu_pack/assets/onepiece/sounds/gomu/bounce_1.ogg    (… bounce_2)
meu_pack/assets/onepiece/sounds/gomu/inflate_1.ogg   (… inflate_2)
```

Os demais sons seguem a mesma ideia (`combat/`, `haki/`, `destruction/`, `fruit/`). Veja a lista completa em `src/main/resources/assets/onepiece/sounds.json`. Coloque o pack em `resourcepacks/` e ative-o em Opções → Pacotes de Recursos.

## Ferramentas
`tools/gen_sounds.py` (sons sintetizados proceduralmente; a borracha usa ressonadores e twang/boing/rangido com variações), `tools/gen_animations.py` (as animações JSON), `tools/gen_textures.py` (ícones/partículas), `tools/gen_test_structure.py` (arena dos testes). Os sons e texturas atuais são *placeholders* originais; cada evento tem arquivo próprio para ser substituído por um resource pack.
