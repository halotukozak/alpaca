%%%%%% -*- Coding: utf-8-unix; Mode: latex

% Engineering thesis template, Faculty of Computer Science, AGH University of Krakow

% Version: 5.0
% Version date: 2023.12.20

\NeedsTeXFormat{LaTeX2e} %
\ProvidesClass{aghengthesis}

\def\titlePL#1        {\gdef\@titlePL{#1}}
\def\titleEN#1        {\gdef\@titleEN{#1}}
\def\faculty#1      {\gdef\@faculty{#1}}
\def\institute#1      {\gdef\@institute{#1}}
\def\thesistype#1   {\gdef\@thesistype{#1}}
\def\fieldofstudy#1   {\gdef\@fieldofstudy{#1}}
\def\typeofstudies#1   {\gdef\@typeofstudies{#1}}
\def\supervisor#1   {\gdef\@supervisor{#1}}

% DECLARING OPTIONS

\DeclareOption{polish}
{
  \newcommand*{\LangPrimaryTitle}{\@titlePL}
  \newcommand*{\LangSecondaryTitle}{\@titleEN}

  \newcommand*{\ChapterTitleProjectVision}{Cel prac i wizja produktu}
  \newcommand*{\ChapterTitleScope}{Zakres funkcjonalności}	
  \newcommand*{\ChapterTitleRealizationAspects}{Wybrane aspekty realizacji}
  \newcommand*{\ChapterTitleWorkOrganization}{Organizacja pracy}
  \newcommand*{\ChapterTitleResults}{Wyniki projektu}

  \newcommand*{\BabelLang}{polish}

  \newcommand*{\LangListing}{Listing}
  \newcommand*{\LangListingRef}{List.}
  \newcommand*{\LangListOfListings}{Spis listingów}
  
  \newcommand*{\LangAlgorithm}{Algorytm}
  \newcommand*{\LangAlgorithmRef}{Alg.}
  \newcommand*{\LangListOfAlgorithms}{Spis algorytmów}

}

\DeclareOption{english}
{
  \newcommand*{\LangPrimaryTitle}{\@titleEN}
  \newcommand*{\LangSecondaryTitle}{\@titlePL}
	
  \newcommand*{\ChapterTitleProjectVision}{Project goals and vision}
  \newcommand*{\ChapterTitleScope}{Functional scope}
  \newcommand*{\ChapterTitleRealizationAspects}{Selected realization aspects}
  \newcommand*{\ChapterTitleWorkOrganization}{Work organization}
  \newcommand*{\ChapterTitleResults}{Project results}


  \newcommand*{\BabelLang}{english}

  \newcommand*{\LangListing}{Listing}
  \newcommand*{\LangListingRef}{List.}
  \newcommand*{\LangListOfListings}{List of Listings}

  \newcommand*{\LangAlgorithm}{Algorithm}
  \newcommand*{\LangAlgorithmRef}{Alg.}
  \newcommand*{\LangListOfAlgorithms}{List of Algorithms}

}

\newcommand*{\LangAgh}{Akademia Górniczo-Hutnicza im. Stanisława Staszica
  w Krakowie}

\newcommand*{\LangFaculty}{Wydział Informatyki}

\newcommand*{\LangInstitute}{Instytut Informatyki}

\newcommand*{\LangFieldOfStudy}{Kierunek studiów: }

\newcommand*{\LangTypeOfStudies}{Typ studiów: }

\newcommand*{\LangSupervisor}{Opiekun pracy: }

\newcommand*{\LangThesisAuthor}{Autorzy: }

\newcommand*{\LangStatementBody}{Uprzedzony(-a) o odpowiedzialności
  karnej na podstawie art. 115 ust. 1 i 2 ustawy z dnia 4 lutego 1994 r. o
  prawie autorskim i prawach pokrewnych (t.j. Dz.U. z 2018 r. poz. 1191 z
  późn. zm.): ,,Kto przywłaszcza sobie autorstwo albo wprowadza w błąd co do
  autorstwa całości lub części cudzego utworu albo artystycznego wykonania,
  podlega grzywnie, karze ograniczenia wolności albo pozbawienia wolności do lat
  3. Tej samej karze podlega, kto rozpowszechnia bez podania nazwiska lub
  pseudonimu twórcy cudzy utwór w wersji oryginalnej albo w postaci opracowania,
  artystyczne wykonanie albo publicznie zniekształca taki utwór, artystyczne
  wykonanie, fonogram, wideogram lub nadanie.'', a także uprzedzony(-a) o
  odpowiedzialności dyscyplinarnej na podstawie art. 307 ust. 1 ustawy z dnia 20
  lipca 2018 r. Prawo o szkolnictwie wyższym i nauce (Dz. U.  z 2018
  r. poz. 1668 z późn. zm.) ,,Student podlega odpowiedzialności dyscyplinarnej za
  naruszenie przepisów obowiązujących w uczelni oraz za czyn uchybiający
  godności studenta.'', oświadczam, że niniejszą pracę dyplomową wykonałem(-am)
  osobiście i samodzielnie i nie korzystałem(-am) ze źródeł innych niż
  wymienione w pracy.

  Jednocześnie Uczelnia informuje, że zgodnie z art. 15a
  ww. ustawy o prawie autorskim i prawach pokrewnych Uczelni przysługuje
  pierwszeństwo w opublikowaniu pracy dyplomowej studenta.  Jeżeli Uczelnia nie
  opublikowała pracy dyplomowej w terminie 6 miesięcy od dnia jej obrony, autor
  może ją opublikować, chyba że praca jest częścią utworu zbiorowego. Ponadto
  Uczelnia jako podmiot, o którym mowa w art. 7 ust. 1 pkt 1 ustawy z dnia 20
  lipca 2018 r. -- Prawo o szkolnictwie wyższym i nauce (Dz. U. z 2018
  r. poz. 1668 z późn. zm.), może korzystać bez wynagrodzenia i bez konieczności
  uzyskania zgody autora z utworu stworzonego przez studenta w wyniku
  wykonywania obowiązków związanych z odbywaniem studiów, udostępniać utwór
  ministrowi właściwemu do spraw szkolnictwa wyższego i nauki oraz korzystać z
  utworów znajdujących się w prowadzonych przez niego bazach danych, w celu
  sprawdzania z wykorzystaniem systemu antyplagiatowego. Minister właściwy do
  spraw szkolnictwa wyższego i nauki może korzystać z prac dyplomowych
  znajdujących się w prowadzonych przez niego bazach danych w zakresie
  niezbędnym do zapewnienia prawidłowego utrzymania i rozwoju tych baz oraz
  współpracujących z nimi systemów informatycznych.}

\newcommand*{\LangSignature}{(czytelny podpis studenta)}

\newcommand*{\LangThesisType}{Projekt dyplomowy}

\DeclareOption*{\PassOptionsToClass{\CurrentOption}{article}}


% PROCESSING OPTIONS
\ProcessOptions
\LoadClass[a4paper,12pt]{report}

\RequirePackage{setspace}
\RequirePackage{times}
\RequirePackage[T1]{fontenc}
\RequirePackage{graphicx}
\RequirePackage{geometry}
\RequirePackage{fancyhdr}
\RequirePackage{titlesec}
\RequirePackage{amssymb}
\RequirePackage[\BabelLang]{babel}
\RequirePackage{amsmath}
\RequirePackage{mathptmx}

\addto\captionspolish{%
  \def\listtablename{Spis tabel}%
  \def\tablename{Tabela}%
}

\geometry{bindingoffset=10mm, lmargin=20mm, rmargin=20mm, tmargin=25mm, bmargin=25mm, foot=10mm, head=10mm}

\renewcommand{\baselinestretch}{1.1}

%------------------------------------------------------------------------------
\renewcommand{\maketitle}{%

\newpage \titlepage

\begin{center}

  \includegraphics[width=120pt,viewport=250 350 350 500]{agh.pdf}

  {\normalsize{\scshape\bfseries \LangAgh}}\\
  \vspace*{1cm}

  {\large{\bfseries \LangFaculty}}\\
  %\vspace*{1mm}
  %{\normalsize\scshape \LangInstitute}

  \vspace*{2.5cm}

  {\LARGE \LangThesisType}\\

  \vspace*{1.5cm}

  \doublespacing
  {\Large\itshape \LangPrimaryTitle}\\

  \vspace*{0.3cm}

  \doublespacing
  {\Large\itshape \LangSecondaryTitle}

\end{center}

\singlespacing
\vfill
\vspace*{1cm}

\begin{tabbing}
\small{\LangThesisAuthor} \hspace{2cm} \= {\@author} \\
\small{\LangFieldOfStudy} \> {\@fieldofstudy}\\
%\small{\LangTypeOfStudies} \> {\@typeofstudies}\\
\small{\LangSupervisor} \> {\@supervisor}
\end{tabbing}

\vfill
\vspace*{1cm}

\begin{center}

  {\small Kraków, \@date}

\end{center}

\newpage
\thispagestyle{empty} 
\mbox{}
% {~~}\vspace{20mm}

% \LangStatementBody

% \vspace{2cm}

% \begin{flushright}
% \makebox[6cm][s]{\dotfill}\par
% \makebox[6cm][c]{\small \LangSignature}
% \end{flushright}


%------------------------------------------------------------------------------

\clearpage
\setcounter{page}{3}

}

%\ProcessOptions

\pagestyle{fancy}
\renewcommand{\sectionmark}[1]{\markright{\thesection.\ #1}}
\renewcommand{\headrulewidth}{0.5pt}
\fancyhf{}
\fancyhead{}

\fancyhead[LE]{\bfseries \thepage}
\fancyhead[RO]{\bfseries \thepage}

\frenchspacing
\sloppy
\widowpenalty     9999
\clubpenalty      9999
\flushbottom

\titleformat{\section}[block]
{\bfseries\Large}{\filright \Large\thesection. }{0ex}{}
\titleformat{\subsection}[block]
{\bfseries\large}{\filright \large\thesubsection. }{0ex}{}
\titleformat{\subsubsection}[block]
{\bfseries\normalsize}{\filright \normalsize\thesubsubsection. }{0ex}{}
