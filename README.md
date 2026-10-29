# Vigie (application Android)

Vigie surveille les mises à jour des équipements et logiciels d'infrastructure : firewalls, switchs,
systèmes serveurs, services. L'application affiche les dernières versions des branches suivies, leurs
dates de fin de support et les avis de sécurité des éditeurs. Elle compare aussi un inventaire local
d'équipements à ces données pour répondre à la question « suis-je à jour ? ».

L'application **ne collecte rien** elle-même. Elle lit les JSON statiques publiés par
[`vigie-collector`](https://github.com/Tchyze/vigie-collector) sur GitHub Pages. Le format de ces
fichiers est défini par le collecteur (`SCHEMA.md` et `schema/*.schema.json`, `schema_version` 1).

## Confidentialité

- L'inventaire et les préférences restent **uniquement sur le téléphone**. L'application n'a ni
  synchronisation, ni outil d'analyse, ni SDK tiers en dehors d'AndroidX, Ktor et kotlinx.
- La seule requête réseau est le téléchargement des JSON publics du collecteur.
- La sauvegarde automatique Android et le transfert d'appareil sont désactivés
  (`allowBackup="false"`, `data_extraction_rules.xml`). Pour changer de téléphone, utilisez l'export
  manuel de l'inventaire dans les réglages.

## Environnement sous WSL (Ubuntu)

Aucun `sudo` n'est nécessaire : tout s'installe dans le dossier personnel.

### JDK 17

```bash
mkdir -p ~/.local && curl -sSL -o /tmp/jdk17.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
```

```bash
tar -xzf /tmp/jdk17.tar.gz -C ~/.local && ln -sfn ~/.local/jdk-17.* ~/.local/jdk-17
```

### SDK Android (outils en ligne de commande)

```bash
mkdir -p ~/Android/Sdk/cmdline-tools && curl -sSL -o /tmp/clt.zip https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip
```

```bash
python3 -m zipfile -e /tmp/clt.zip /tmp/clt && mv /tmp/clt/cmdline-tools ~/Android/Sdk/cmdline-tools/latest && chmod +x ~/Android/Sdk/cmdline-tools/latest/bin/*
```

```bash
JAVA_HOME=~/.local/jdk-17 ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=$HOME/Android/Sdk "platforms;android-37.0" "build-tools;37.0.0" "platform-tools"
```

### Variables d'environnement

Ajoutez ces lignes à la fin de `~/.bashrc`, puis ouvrez un nouveau terminal :

```bash
export JAVA_HOME="$HOME/.local/jdk-17"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Si `local.properties` n'existe pas, créez-le à la racine du projet (il n'est pas versionné) :

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

## Build

Gradle passe par le wrapper (`./gradlew`) : il n'y a rien à installer.

```bash
./gradlew testDebugUnitTest assembleDebug
```

L'APK est généré dans `app/build/outputs/apk/debug/app-debug.apk`.

## Installation sur le téléphone

L'émulateur fonctionne mal sous WSL : on teste sur un vrai téléphone.

### Méthode simple : copier l'APK

1. Copiez l'APK dans le dossier Téléchargements de Windows :

   ```bash
   cp app/build/outputs/apk/debug/app-debug.apk /mnt/c/Users/bacht/Downloads/vigie-debug.apk
   ```

2. Envoyez le fichier sur le téléphone, par câble USB (mode transfert de fichiers), Quick Share ou
   tout autre moyen.
3. Sur le téléphone, ouvrez le fichier. Android demande d'autoriser l'installation depuis
   l'application utilisée (Fichiers, navigateur…) : acceptez pour cette application uniquement.
4. Pour une nouvelle version, refaites la même chose : l'installation remplace l'ancienne version
   **sans effacer les données**, car toutes les versions debug sont signées par la même clé locale
   (`~/.android/debug.keystore`).

### Avec ADB (pratique si vous testez souvent)

ADB doit tourner côté Windows, car WSL ne voit pas les ports USB.

1. Téléchargez les
   [Platform-Tools pour Windows](https://developer.android.com/tools/releases/platform-tools) et
   décompressez-les, par exemple dans `C:\platform-tools`.
2. Sur le téléphone : *Paramètres › À propos › Numéro de build* (touchez 7 fois), puis
   *Options pour les développeurs › Débogage USB*.
3. Branchez le téléphone et acceptez l'empreinte de l'ordinateur. Ensuite, depuis WSL :

   ```bash
   /mnt/c/platform-tools/adb.exe install -r "$(wslpath -w app/build/outputs/apk/debug/app-debug.apk)"
   ```

### Depuis GitHub Actions

Chaque push lance les tests puis le build. L'APK est disponible en artefact `vigie-debug-apk` sur la
page du workflow (onglet *Actions*). Attention : il est signé par la clé debug du runner, différente de
la vôtre. Il faut donc désinstaller une version construite localement avant de l'installer, ce qui
**efface l'inventaire** si vous ne l'avez pas exporté.

## Lien avec `vigie-collector`

| Fichier publié | Utilisation dans l'application |
|---|---|
| `index.json` | liste des produits et fichiers à charger |
| `products/<produit>.json` | branches, versions et dates de fin de support |
| `advisories.json` | avis de sécurité |

L'URL de base se règle dans l'application. Par défaut : `https://tchyze.github.io/vigie-collector/`.
Tant que le collecteur n'est pas publié, le **mode démo** utilise des JSON d'exemple conformes au
schéma, embarqués dans l'application (`app/src/main/assets/demo/`).

Si un fichier publié porte un `schema_version` que l'application ne connaît pas, elle affiche
« Mise à jour de l'application nécessaire » et continue de travailler sur les dernières données en
cache.
