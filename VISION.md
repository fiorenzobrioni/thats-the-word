# On My Tongue — Vision

> Stato: approvata — 12 novembre 2026

## 1. In una frase

**On My Tongue** è un gioco per Android in cui si indovina una parola nascosta, in italiano, inglese e spagnolo, pensato sia come passatempo quotidiano sia per esercitare il vocabolario di una lingua che si sta imparando. Qualità premium, codice aperto, completamente offline.

## 2. Nome e voce

Il nome richiama "ce l'ho sulla punta della lingua" (*on the tip of my tongue*, *en la punta de la lengua*): un modo di dire comune alle tre lingue, in cui "tongue" è anche la lingua parlata. Il nome resta in inglese in tutte le versioni dell'interfaccia.

Sottotitolo: **A daily word game in three tongues**.

Tono dei testi: breve, calmo, un po' teatrale. Frasi corte, nessun punto esclamativo, nessun gergo da videogioco. Testi di riferimento per la schermata iniziale:

```
ON MY TONGUE
A DAILY WORD GAME IN THREE TONGUES
Today's word awaits.
Six attempts. One word.
T A B L E
THE WORD IS ALMOST YOURS.
```

Ogni testo va scritto nelle tre lingue dell'interfaccia con lo stesso ritmo, non tradotto alla lettera.

## 3. Principi

1. **Poche cose, fatte benissimo.** Meglio una modalità rifinita che tre abbozzate.
2. **La lingua straniera è il cuore.** Ciò che distingue l'app è poter giocare in una lingua non propria senza frustrazione, e uscirne avendo imparato qualcosa.
3. **Offline e rispettosa.** Nessun account, nessuna pubblicità, nessun tracciamento. L'app non richiede il permesso di accesso a Internet.
4. **Aperta.** Codice sotto licenza GPL-3.0-or-later, dati linguistici da fonti libere e documentate, build prodotta dalla CI.
5. **Identità propria.** Nome, aspetto, colori e liste di parole originali, non ripresi da giochi esistenti.

## 4. A chi si rivolge

- Chi vuole un passatempo di parole curato e senza pubblicità.
- Chi studia italiano, inglese o spagnolo e vuole un esercizio leggero e quotidiano.

Promessa onesta: è un esercizio di vocabolario, non un corso di lingua.

## 5. Lingue

- **Interfaccia**: italiano, inglese, spagnolo. Segue la lingua del dispositivo e si può cambiare dalle impostazioni.
- **Lingua di gioco**: italiano, inglese, spagnolo, scelta in modo indipendente dall'interfaccia e modificabile in qualsiasi momento.
- Traduzioni, definizioni, aiuti e testi di contorno sono sempre mostrati **nella lingua dell'interfaccia**.
- **Gioco in lingua straniera**: quando la lingua di gioco è diversa da quella dell'interfaccia, l'app attiva gli strumenti di pratica (sezione 9).
- **Gioco nella propria lingua**: quando le due lingue coincidono, il gioco è un passatempo classico: nessun aiuto, e a fine partita la definizione al posto della traduzione.

## 6. Regole di base

- Una parola nascosta da indovinare in un numero limitato di tentativi (6 nella modalità classica a 5 lettere).
- Ogni tentativo deve essere una parola valida del vocabolario.
- Dopo ogni tentativo ogni lettera riceve un riscontro: posizione giusta, presente altrove, assente.
- Il riscontro è comunicato con **colore e forma insieme**, mai con il solo colore.

### Caratteri speciali

- **Italiano**: gli accenti si ignorano in digitazione; la forma corretta (es. *città*) è mostrata a fine partita.
- **Spagnolo**: la **ñ** è una lettera a sé con un proprio tasto; vocali accentate e dieresi si ignorano in digitazione.
- **Inglese**: sono accettate le grafie britannica e americana; le parole con doppia grafia sono escluse dalle soluzioni.

## 7. Struttura dell'app

| Schermata | Contenuto |
|---|---|
| **Inizio** | La parola del giorno in evidenza per la lingua di gioco attiva, cambio rapido di lingua, calendario delle giornaliere, accesso alle altre modalità. |
| **Partita** | Griglia, tastiera con lo stato delle lettere, aiuti (se disponibili), scheda di fine partita. |
| **Quaderno** | Le parole incontrate, con ricerca e ripasso. |
| **Progressi** | Statistiche, serie e traguardi. |
| **Impostazioni** | Lingue, livello di vocabolario, opzione Difficile, suoni, vibrazione, animazioni, backup, licenze e attribuzioni. |

## 8. Modalità di gioco

### Versione 1.0 — il nucleo

| Modalità | Descrizione |
|---|---|
| **Giornaliera** | Una parola al giorno per ciascuna lingua, uguale per tutti. È calcolata dalla data su una lista ordinata e congelata, quindi senza server. Cambia alla mezzanotte locale. |
| **Archivio** | Un calendario per recuperare le giornaliere saltate o rigiocare una data specifica, dalla prima giornaliera dell'app fino a oggi. Le date future non sono giocabili. Il calendario mostra per ogni giorno se la parola è stata risolta, mancata o non giocata. |
| **Libera** | Partite illimitate, con scelta del livello di vocabolario. |
| **Opzione Difficile** | Gli indizi ottenuti vanno riusati nei tentativi successivi. Attivabile in Giornaliera e Libera. |

Tutto a 5 lettere e 6 tentativi. Inclusi fin da subito: livelli di vocabolario, aiuti, scheda di fine partita, Quaderno, statistiche, serie, traguardi, backup.

### Versione 1.1 — le modalità linguistiche

| Modalità | Descrizione |
|---|---|
| **Traduci** | Viene mostrata una parola nella lingua dell'interfaccia; si indovina la sua traduzione nella lingua di gioco, con meno tentativi (indicativamente 4). Allena il richiamo attivo. |
| **Ponte** | Tre griglie, lo stesso concetto nelle tre lingue (*gatto / cat / gato*), con lunghezze diverse. Risolverne una rivela il significato e aiuta con le altre. È la modalità-firma dell'app. |
| **Ripasso** | Ripropone le parole del Quaderno a distanza crescente di giorni. |

### Versione 1.2 e oltre — le varianti

| Modalità | Descrizione |
|---|---|
| **Scalata** | Una serie di parole con un monte tentativi complessivo; ogni soluzione diventa il primo tentativo della parola successiva. |
| **Più griglie** | Due o quattro parole da indovinare in parallelo con gli stessi tentativi. |
| **Lunghezza variabile** | Parole da 4 a 7 lettere. |
| **Bugiarda** | In ogni riga un riscontro è falso. |

L'ordine riflette il costo: ogni nuova lunghezza moltiplica le liste di parole da preparare. Il numero di tentativi delle modalità oltre la classica va tarato giocando i prototipi.

## 9. Praticare una lingua

### Livelli di vocabolario

Le soluzioni sono suddivise per frequenza d'uso: **Base**, **Intermedio**, **Avanzato**, **Completo**. In una lingua straniera si parte da Base. La Giornaliera pesca sempre da parole comuni, perché deve essere uguale per tutti.

### Aiuti

Disponibili solo quando si gioca in una lingua diversa da quella dell'interfaccia:

- **Traduzione**: mostra la traduzione della soluzione, al costo di un tentativo.
- **Definizione**: mostra una definizione nella lingua dell'interfaccia, al costo di un tentativo.
- **Lettera**: rivela una lettera in posizione corretta, al costo di un tentativo.

Le partite giocate con aiuti valgono per la serie ma sono contrassegnate nelle statistiche.

### Scheda di fine partita

Per la soluzione e per ogni parola tentata: significato, traduzione nella lingua dell'interfaccia, forma con accenti corretti. Se una traduzione manca, viene mostrata la sola definizione.

### Quaderno

- Raccoglie automaticamente le parole incontrate, con traduzione, lingua e data.
- Segna quali parole sono state indovinate, mancate o risolte con aiuti.
- Consultabile e ricercabile come un piccolo dizionario personale.
- Alimenta la modalità Ripasso.

## 10. Vocabolari

Obiettivo: vocabolari **il più ampi possibile**, con una distinzione netta tra due insiemi.

| Insieme | Contenuto | Criterio |
|---|---|---|
| **Parole accettate** | Tutte le forme valide, comprese forme flesse (plurali, coniugazioni) | Il più ampio possibile: meno rifiuti ingiusti |
| **Soluzioni** | Solo forme base, senza nomi propri, sigle e termini offensivi | Ampio ma ordinato per livello; le parole rare stanno in Avanzato e Completo |

### Fonti previste

| Dato | Fonte | Licenza |
|---|---|---|
| Parole italiane | Dizionario Hunspell italiano (versione LibreOffice), espanso in tutte le forme | GPL 3 |
| Parole spagnole | Dizionario Hunspell spagnolo | Da verificare |
| Parole inglesi | SCOWL o ENABLE | Da verificare |
| Frequenze d'uso | wordfreq, liste OpenSubtitles, Leipzig Corpora | CC BY-SA 4.0 / CC BY 4.0 |
| Definizioni e traduzioni | Wiktionary, tramite i dati estratti da kaikki.org | CC BY-SA e GFDL |

### Regole sui dati

- I vocabolari sono generati da uno **script riproducibile** incluso nel repository: fonti, versioni e filtri sono tracciati.
- I dati sono distribuiti come file separati, ciascuno con la propria licenza e attribuzione, elencate in un file dedicato e in una schermata dell'app.
- Le liste delle soluzioni giornaliere sono **congelate**: gli aggiornamenti aggiungono in coda e non cambiano le parole già assegnate a una data.
- La copertura delle traduzioni sarà incompleta per le parole rare: le parole senza traduzione affidabile non entrano in Traduci e Ponte.
- Ponte richiede terne di concetti allineate nelle tre lingue: generate automaticamente tra le parole comuni e poi **riviste a mano**.

## 11. Progressione

- **Statistiche** per lingua e modalità: partite, percentuale di successo, distribuzione dei tentativi, uso degli aiuti.
- **Serie** giornaliera, per lingua. Conta solo le parole risolte nel loro giorno: le giornaliere recuperate dall'Archivio entrano nelle statistiche e nel calendario ma non riparano una serie interrotta.
- **Traguardi**: obiettivi che premiano costanza e apprendimento, ad esempio "7 giornaliere di fila", "prima vittoria in una lingua straniera", "100 parole nel Quaderno", "giornaliera risolta in tutte e tre le lingue nello stesso giorno".
- **Nessun contenuto sbloccabile**: niente temi o premi; i traguardi sono un riconoscimento, non una chiave.
- **Backup**: esportazione e importazione di statistiche, traguardi e Quaderno in un file, dato che non esistono account.
- **Condivisione** del risultato senza rivelare la parola, in un formato grafico proprio dell'app.

## 12. Esperienza premium

- **Grafica più curata e più animazioni** rispetto alle app gemelle, come si addice a un gioco.
- **Animazioni** per ogni momento chiave: inserimento della lettera, rivelazione del riscontro, parola non valida, vittoria, traguardo raggiunto.
- **Vibrazione** e **suoni** discreti, disattivabili separatamente.
- **Prima partita guidata**: le regole si capiscono giocando, senza una schermata di istruzioni.
- **Accessibilità**: forme oltre ai colori, supporto a TalkBack, testo ridimensionabile, opzione per ridurre le animazioni.
- **Coerenza con le app gemelle**: stile, tipografia e componenti condivisi con le altre app dell'ecosistema.
- Tema chiaro e scuro; i colori del riscontro sono fissi e propri dell'app.

## 13. Identità e aspetti legali

Nota: queste sono valutazioni informate, non un parere legale.

- **Regole di gioco**: le meccaniche non sono protette da diritto d'autore; lo sono nomi, codice, testi e grafica specifica.
- **"Wordle" è un marchio del New York Times.** Nel 2024 il NYT ha inviato numerose richieste di rimozione a progetti su GitHub, contestando l'uso del nome e dell'aspetto (disposizione della griglia e schema verde/grigio/giallo). Il NYT ha dichiarato di non avere obiezioni verso giochi di parole simili che non violino marchio e opere protette.
- **Conseguenze per questo progetto**:
  - quel nome non è mai usato per presentare o descrivere l'app: né nell'interfaccia, né nel README, né nelle note di rilascio, né nel codice; questa nota legale e il relativo link nei Riferimenti sono gli unici punti del repository in cui compare;
  - nessun codice, testo o lista di parole di quella origine;
  - palette, forme e stile propri, derivati dal tema dell'ecosistema;
  - il rischio residuo è basso ma non nullo, perché il repository è pubblico su GitHub.
- **Licenza del codice**: GPL-3.0-or-later (GPL versione 3 o successive). Nome e logo dell'app non sono coperti dalla licenza.
- **Licenze dei dati**: compatibili con la distribuzione insieme a codice GPLv3 (la CC BY-SA 4.0 è dichiarata compatibile in senso unico con la GPLv3).
- **Dipendenze**: nessuna libreria proprietaria (niente pubblicità, analisi o servizi Google Play).

## 14. Tecnologia e distribuzione

- **Stack**: Kotlin, Jetpack Compose, Material 3 Expressive, da allineare alle app gemelle alla creazione del repository.
- **Motore di gioco** in Kotlin puro, separato dall'interfaccia e coperto da test.
- **Dati** inclusi nell'app in formato compatto; nessun download.
- **CI su GitHub**: compilazione e firma con chiave propria tramite secret, secondo la procedura delle app gemelle.
- **Distribuzione**: release su GitHub (compatibili con Obtainium). Il Play Store è previsto nel piano ma non programmato; F-Droid è un'opzione.
- **Sviluppo** assistito da Claude Code a partire da questo documento e da PLANNING.md.

## 15. Fuori ambito

- Multigiocatore, classifiche online, account, sincronizzazione cloud.
- Pubblicità, acquisti in-app, temi o contenuti sbloccabili.
- Altri giochi: l'app fa una cosa sola.
- Altre lingue oltre alle tre previste (l'architettura non deve però impedirle).

## 16. Questioni aperte

1. **Disponibilità del nome.** Una ricerca web non ha trovato app chiamate "On My Tongue", ma store e registri dei marchi vanno controllati a mano prima della prima release.
2. **Nomi delle modalità** nelle tre lingue dell'interfaccia.
3. **Licenze esatte** delle fonti spagnola e inglese e versione della licenza dei dati Wiktionary.
4. **Qualità delle definizioni** nelle tre lingue dell'interfaccia: da valutare su un campione prima di impegnarsi.
5. **Dimensione dell'app** con tre vocabolari completi e definizioni: da misurare con un prototipo dei dati.

## Riferimenti

- Richieste di rimozione del NYT (AP, 2024): https://wjla.com/news/offbeat/nyt-fighting-off-wordle-copycats-with-several-takedown-notices-word-guessing-game-new-york-times-look-alikes-digital-millenium-copycat-act-github-trademark-battle
- Compatibilità CC BY-SA 4.0 → GPLv3: https://creativecommons.org/2015/10/08/cc-by-sa-4-0-now-one-way-compatible-with-gplv3/
- Dizionario italiano LibreOffice: https://github.com/LibreOffice/dictionaries/tree/master/it_IT
- wordfreq: https://github.com/rspeer/wordfreq
- Liste di frequenza (Wiktionary): https://en.wiktionary.org/wiki/Wiktionary:Frequency_lists
- Dati Wiktionary estratti: https://kaikki.org/dictionary/rawdata.html
