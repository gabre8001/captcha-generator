# captcha-generator

Java와 OpenCV를 활용하여 텍스트 기반 CAPTCHA 이미지를 생성하는 커맨드라인 도구입니다.

## 예시 이미지

생성된 CAPTCHA 이미지는 아래와 같은 형태입니다:

- 6자리 랜덤 문자 (혼동하기 쉬운 `0/O`, `1/l/I` 제외)
- 글자마다 무작위 크기·색상·회전 적용
- OpenCV Perspective Warp로 이미지 전체 왜곡
- 3차 베지어 곡선이 문자 위를 가로지르는 노이즈 선 추가
- 배경 노이즈 점 포함

이미지 크기: **220 × 80 px** (PNG)

## 요구 사항

| 항목 | 버전 |
|------|------|
| Java | 21 이상 |
| Gradle | 8.x (Wrapper 포함) |

> OpenCV 네이티브 라이브러리는 `org.openpnp:opencv:4.9.0-0` 의존성에 번들되어 있어 별도 설치가 필요 없습니다.

## 빠른 시작

### 1. 저장소 클론

```bash
git clone https://github.com/gabre8001/captcha-generator.git
cd captcha-generator
```

### 2. Gradle로 바로 실행

```bash
# Windows
gradlew.bat run

# macOS / Linux
./gradlew run
```

기본값으로 `output/` 폴더에 CAPTCHA 이미지 **5장**이 생성됩니다.

## 이미지 생성 방법

### 방법 1: Gradle run (개발·테스트용)

```bash
# 기본: output/ 폴더에 5장 생성
gradlew.bat run

# 인수 지정: [출력 폴더] [생성 수]
gradlew.bat run --args="my-output 20"
```

### 방법 2: Fat JAR 빌드 후 실행 (배포·배치용)

```bash
# 1) 모든 의존성이 포함된 Fat JAR 빌드
gradlew.bat jar

# 2) JAR 실행
java -jar build/libs/captcha-generator-1.0.0.jar

# 인수 지정
java -jar build/libs/captcha-generator-1.0.0.jar my-output 20
```

### 실행 인수

```
[출력 폴더]  생성된 이미지를 저장할 경로 (기본값: output)
[생성 수]    생성할 이미지 수 (기본값: 5)
```

### 출력 파일명 규칙

```
{출력 폴더}/captcha_{텍스트}.png

예시) output/captcha_aB3dEf.png
```

## 프로젝트 구조

```
captcha-generator/
├── src/main/java/com/captcha/
│   ├── CaptchaGenerator.java   # 이미지 생성 핵심 로직
│   └── Main.java               # 진입점 (CLI 인수 처리)
├── build.gradle
├── settings.gradle
└── gradlew.bat
```

## 기술 스택

- **Java 21**
- **Gradle 8** (Application Plugin, Fat JAR)
- **OpenCV 4.9.0** (`org.openpnp:opencv`) — Perspective Warp, Bezier 곡선 렌더링
- **Java2D (Graphics2D)** — 텍스트 렌더링

## 라이선스

MIT
