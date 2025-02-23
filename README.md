# GCP-Assistant 🚀
GCP-Assistant는 **Google Cloud Platform (GCP) 관리 기능을 지원하는 디스코드 봇**입니다.  
디스코드 서버에서 간편한 명령어를 통해 GCP VM을 관리하고, 비용 예측 및 로그 확인이 가능합니다.  

---

## 📌 주요 기능 🛠️
✅ **GCP VM 제어** (`start`, `stop`)  
✅ **GCP VM 로그 확인** (`logs`)  
✅ **예상 비용 조회** (`cost`)  
✅ **VM 상태 변경 알림** (`notify`)  

---

## 📌 명령어 사용법 💬
> `[]`는 필수 입력값, `{}`는 선택 입력값입니다.

| 명령어 | 설명 | 예제 |
|--------|------|------|
| `/gcp start [vm_name]` | 지정한 GCP VM을 시작 | `/gcp start my-vm` |
| `/gcp stop [vm_name]` | 지정한 GCP VM을 중지 | `/gcp stop my-vm` |
| `/gcp logs` | 최근 GCP VM 로그 확인 | `/gcp logs` |
| `/gcp cost` | 예상 GCP 비용 조회 | `/gcp cost` |
| `/gcp notify` | VM 상태 변경 시 알림 활성화 | `/gcp notify` |

---
