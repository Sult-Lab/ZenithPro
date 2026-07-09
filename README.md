# ZenithPro

ZenithPro is a comprehensive business management and Point of Sale (POS) solution designed for Android. It empowers business owners to manage inventory, sales, customers, and expenses all in one place with a modern, intuitive interface.

## 🚀 Features

- **Dynamic Dashboard**: Real-time overview of business performance with quick action shortcuts.
- **Inventory Management**: Effortlessly add and track products, manage categories, and handle stock levels.
- **Smart POS & Checkout**: Streamlined sales process with barcode scanning (CameraX + ML Kit) and receipt preview.
- **Payment Integrations**: Support for multiple payment methods, including Nomba bank transfers.
- **Customer CRM**: Maintain a detailed customer database and view customer-specific purchase reports.
- **Expense Tracking**: Record and categorize business expenses to keep a close eye on profitability.
- **Multi-Branch Support**: Manage multiple business locations from a single app.
- **Staff Management**: Create and manage staff accounts with role-based access.
- **Professional Reporting**: Generate analytics for sales, inventory, and overall business health.
- **Print Support**: Integrated printer settings for generating physical receipts.

## 🛠 Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (100% Declarative UI)
- **Architecture**: Clean Architecture with MVI/MVVM patterns.
- **Backend**: [Supabase](https://supabase.com/) (Authentication, Postgrest, and Edge Functions)
- **Dependency Injection**: [Koin](https://insert-koin.io/)
- **Navigation**: [Navigation 3](https://developer.android.com/jetpack/compose/navigation) (Experimental/Alpha)
- **Database**: [Room](https://developer.android.com/training/data-storage/room) for local persistence and offline support.
- **Networking**: [Ktor](https://ktor.io/) & Supabase Kotlin SDK.
- **Scanning**: [CameraX](https://developer.android.com/training/camerax) & [Google ML Kit](https://developers.google.com/ml-kit) for Barcode scanning.
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Local Storage**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for user preferences.

## 🔑 Test Credentials (For Judges)

To explore the full functionality of the app, please use the following credentials:

| Role | Email                      | Password   |
| :--- |:---------------------------|:-----------|
| **Admin/Owner** | `salamsultan09@gmail.com`  | `Test1234` |
| **Staff** | `unde5inedbrand@gmail.com` | `Test1234` |

*Note: The admin account has full access to settings, staff management, and financial reports, while the staff account is restricted to sales and inventory viewing.*

## 📸 Screenshots

*(Add your screenshots here to showcase the UI)*

## 🏗 Setup & Installation

1. Clone the repository.
2. Open the project in **Android Studio Ladybug** or newer.
3. Ensure you have the latest stable Android SDK installed.
4. Build and run the `:app` module on an emulator or physical device.

---

Built with ❤️ by TechSultan.
