# SignalVFX

> 에디터로 만드는 마인크래프트 스킬(마법) 시스템 — MagicSpells 스타일

**SignalVFX**는 데스크톱 에디터로 스킬(마법)을 디자인하고, 그 결과물(JSON)을 Paper 서버 플러그인이 그대로 읽어 실행하는 시스템입니다.
비주얼은 **리소스팩(기본)** 또는 **디스플레이 엔티티(빌보드 커스텀 파티클)** 중에서 고르고,
데미지는 에디터에서 **포인트를 찍고 범위를 설정**하는 방식으로 만듭니다.

이 문서는 **계획서**를 겸합니다. 무엇을 왜 이렇게 만들고, 지금 어디까지 됐고, 다음에 뭘 할지를 정리합니다.

---

## 1. 핵심 컨셉

| 항목 | 내용 |
|---|---|
| 대상 서버 | **Paper 1.21.x** (Bukkit API, 디스플레이 엔티티 1.19.4+) |
| 에디터 | **JavaFX 데스크톱 앱** |
| 스킬 정의 | 사람이 읽을 수 있는 **JSON** (에디터와 플러그인이 공유하는 단일 계약) |
| 비주얼 A (기본) | **리소스팩** — 커스텀 아이템 모델/애니메이션. 플러그인이 팩을 **호스팅·자동 적용** |
| 비주얼 B | **디스플레이 엔티티** — 팩 없이 서버가 스폰. **빌보드**로 커스텀 파티클처럼 연출 |
| 데미지 | 스킬당 여러 **데미지 포인트**(오프셋 + 범위 도형 + 데미지 + 타이밍) |

한 스킬은 비주얼을 **정확히 하나** 선택하고, 데미지 포인트는 **여러 개**를 가질 수 있습니다.

---

## 2. 아키텍처 (Maven 멀티모듈)

```
SignalVFX/
├─ pom.xml                # 부모 POM (버전/의존성 관리)
├─ skill-model/           # ✅ 공유 스킬 스펙 + JSON 직렬화 (에디터·플러그인 공용)
├─ editor/                # ✅ JavaFX 데스크톱 에디터
├─ plugin/                # ⏳ Paper 플러그인 (다음 단계)
└─ examples/skills/       # 예제 스킬 JSON (fireball, frost_nova)
```

`skill-model`이 **단일 진실 원천(single source of truth)** 입니다.
에디터가 저장한 JSON을 플러그인이 100% 동일한 클래스로 역직렬화하므로, 스펙이 어긋날 일이 없습니다.

**의존 방향:** `editor → skill-model`, `plugin → skill-model` (에디터와 플러그인은 서로 모름)

---

## 3. 스킬 스펙 (JSON)

`skill-model`의 클래스가 곧 스키마입니다. 최상위 구조:

```jsonc
{
  "schemaVersion": 1,
  "id": "fireball",              // 명령어/설정에서 쓰는 고정 식별자
  "name": "Fireball",
  "description": "...",
  "author": "SignalVFX",
  "icon": "minecraft:fire_charge",
  "cast":    { /* 발동 방식 */ },
  "visual":  { /* 비주얼 (아래 4장) */ },
  "damagePoints": [ /* 데미지 (아래 5장) */ ]
}
```

### 3.1 cast — 발동 설정 (`CastSettings`)
| 필드 | 의미 |
|---|---|
| `type` | `INSTANT` / `CHARGE`(차징) / `CHANNEL`(채널링) |
| `targeting` | `SELF` / `TARGET_ENTITY` / `TARGET_BLOCK` / `PROJECTILE` |
| `castTimeTicks` | 차징 시간 또는 채널링 간격 (20틱 = 1초) |
| `cooldownTicks` | 재사용 대기시간 |
| `range` | 타겟 탐색/투사체 사거리(블록) |
| `cost` | 소모 자원: `type`(`MANA`/`HEALTH`/`HUNGER`/`EXPERIENCE`/`ITEM`/`NONE`), `amount`, `itemKey` |

---

## 4. 비주얼 (VFX)

`visual`은 `type` 판별자로 두 종류 중 하나를 선택합니다. 공통 필드는 `attach`(고정 위치),
`offset`, `durationTicks`, `soundKey`/`soundVolume`/`soundPitch` 입니다.

### 4.1 리소스팩 비주얼 — `"type": "RESOURCE_PACK"` (기본)
커스텀 아이템 모델/애니메이션으로 연출합니다. 서버가 스폰하는 오브젝트는 최소화하고, 룩은 팩이 담당합니다.

| 필드 | 의미 |
|---|---|
| `material` | 모델을 얹을 베이스 아이템 (예: `minecraft:paper`) |
| `itemModel` | 1.21.4+ 아이템 모델 키 (예: `signalvfx:fireball`) — 설정 시 우선 |
| `customModelData` | 구버전 팩용 CMD 셀렉터 |
| `animation` | 팩이 반응할 애니메이션/상태 이름 |
| `scale` | 표시 스케일 |

> **리소스팩 호스팅(계획):** 플러그인이 지정 폴더의 팩을 zip으로 묶어 내장 HTTP로 서빙하고,
> 접속 플레이어에게 `player.setResourcePack(url, hash)`로 자동 적용합니다. (자세히는 6.3)

### 4.2 디스플레이 엔티티 비주얼 — `"type": "DISPLAY_ENTITY"`
리소스팩 없이 서버가 `item_display`/`block_display`/`text_display`를 스폰합니다.
**빌보드를 `CENTER`로 두면 항상 카메라를 바라보므로, 작은 아이템을 여러 개 스폰해 커스텀 파티클처럼** 연출할 수 있습니다.

| 필드 | 의미 |
|---|---|
| `displayKind` | `ITEM` / `BLOCK` / `TEXT` |
| `item` / `block` / `text` | 종류별 표시 대상 |
| `billboard` | `FIXED` / `VERTICAL` / `HORIZONTAL` / **`CENTER`**(파티클 룩) |
| `blockLight` / `skyLight` | 발광 밝기 0–15 (‑1이면 주변광) |
| `glowing` / `glowColor` | 외곽 발광 및 색(`#RRGGBB`) |
| `baseTransform` | `translation`/`scale`/`leftRotation`/`rightRotation` (회전은 Euler 도) |
| `keyframes[]` | `atTick`+`interpolationDurationTicks`+`transform` — 시간에 따른 보간 애니메이션 |

> **커스텀 파티클 패턴:** 짧은 수명 + `CENTER` 빌보드 + 작은 스케일의 아이템 디스플레이를
> 링/폭발 형태로 배치하고 keyframe으로 커지거나 퍼지게 하면, 바닐라 파티클로는 어려운 룩을 팩 없이 만들 수 있습니다.

---

## 5. 데미지 시스템

스킬은 **데미지 포인트**의 배열을 가집니다. 에디터에서 "포인트를 찍고 범위를 설정"하는 것이 곧 이 배열을 만드는 일입니다.

`DamagePoint` 주요 필드:

| 필드 | 의미 |
|---|---|
| `origin` | 기준 좌표계: `CASTER` / `TARGET` / `CAST_DIRECTION` |
| `offset` | 기준점으로부터의 상대 위치 (x/y/z) |
| `shape` | 범위 도형 (아래) |
| `damage` | 반 하트 단위 데미지 |
| `delayTicks` | 스킬 시작 후 발동 지연 |
| `repeatCount` / `repeatIntervalTicks` | 반복 타격(예: 장판 도트딜) |
| `targetFilter` | `ENEMIES` / `ALLIES` / `SELF` / `ALL` / `NOT_CASTER` |
| `knockback` | 바깥 방향 넉백 세기 |
| `ignoreInvulnerability` | 무적(i-frame) 무시 여부 |
| `potionEffects[]` | 적중 시 부여할 포션 효과 |

**범위 도형(`shape`)**
- `SPHERE` — `radius`
- `BOX` — `halfExtents`(x/y/z)
- `CONE` — `angle`(도) + `length`, 시전 방향으로 열림

여러 포인트에 서로 다른 `delayTicks`를 주면 **다단 히트**(참격 → 폭발 등)를 만들 수 있습니다.

---

## 6. 로드맵

### 6.1 ✅ 1단계 — 스킬 모델 (`skill-model`) *(완료)*
- 전체 스펙 클래스 + Jackson 다형성(`visual.type`) 직렬화
- `SkillIO`로 로드/세이브, 라운드트립 검증
- 예제 스킬 2종 생성 (`examples/skills/`)

### 6.2 ✅ 2단계 — 에디터 (`editor`) *(1차 완료)*
- 탭 3개: **Skill & Cast** / **Visual (VFX)** / **Damage**
- 비주얼 탭: 리소스팩 ↔ 디스플레이 엔티티 **토글**(전환 시 공통 필드 보존)
- 데미지 탭: **탑다운 캔버스**에서 포인트를 드래그 배치하고 범위(반경) 시각화, 속성 폼 편집
- 파일 New/Open/Save/Save As, 변경(dirty) 표시
- 향후: keyframe 타임라인 UI, VFX 실시간 미리보기, 아이템/블록 키 자동완성

### 6.3 ⏳ 3단계 — Paper 플러그인 (`plugin`) *(다음)*
계획된 구성 요소:
- **SkillRegistry** — `plugins/SignalVFX/skills/*.json` 로드/리로드(`/svfx reload`)
- **CastManager** — 발동 트리거(아이템 우클릭/명령어), 쿨다운·코스트 체크
- **TargetResolver** — `targeting`에 따라 엔티티/블록/투사체 타겟 산출
- **VFX 렌더러**
  - `ResourcePackRenderer` — 모델 얹은 아이템/투사체 표시
  - `DisplayEntityRenderer` — 디스플레이 스폰 + `baseTransform`/`keyframes` 보간, 빌보드 파티클
- **DamageEngine** — `shape`로 대상 수집 → `targetFilter` 필터 → 데미지/넉백/포션, `delayTicks`·`repeat` 스케줄링
- **ResourcePackHost** — 팩 폴더를 zip으로 묶어 내장 HTTP로 서빙, 접속 시 자동 적용(URL+SHA-1)
- **ManaService** — 마나 자원(선택), PlaceholderAPI 연동(선택)

### 6.4 ⏳ 4단계 — 연동 마감
- 에디터 keyframe 타임라인 + 미리보기
- 스킬 → 아이템/명령어 바인딩, 권한, 파티/팀 판정
- 문서화, 배포용 빌드(플러그인 jar + 에디터 실행본)

---

## 7. 빌드 & 실행

**요구:** JDK 21+, Maven 3.9+

```bash
# 전체 빌드 (모델 + 에디터)
mvn install

# 에디터 실행 (JavaFX)
mvn -pl editor javafx:run

# 또는 패키징된 실행 jar (빌드한 OS용)
java -jar editor/target/signalvfx-editor.jar
```

> JavaFX 네이티브는 OS별로 다릅니다. POM이 mac/win/linux를 자동 감지하며,
> Apple Silicon은 `-Djavafx.platform=mac-aarch64`로 지정하세요.

예제 스킬은 `examples/skills/fireball.json`(리소스팩), `examples/skills/frost_nova.json`(디스플레이 빌보드)에서 확인할 수 있습니다.

---

## 8. 현재 상태 요약

- ✅ 공유 스킬 스펙 + JSON 입출력 (`skill-model`)
- ✅ 데스크톱 에디터 1차 (`editor`) — 3개 탭, 비주얼 토글, 데미지 캔버스
- ✅ 예제 스킬 2종 + 라운드트립 검증
- ⏳ Paper 플러그인 (실행 엔진 · VFX 렌더 · 데미지 · 리소스팩 호스팅) — 다음 단계
