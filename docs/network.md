# Train network

The app starts with **133 stations** and **88 trains**, seeded by `config/RailDataSeeder.java`. Each train has a return working, so every route runs both ways. Timings, distances and coach make-up are close to the real services but are not exact.

Search finds a train when one train calls at both stations, in that order. There are no connecting journeys, so a pair of cities with no train in common returns "no trains". Any two stops on the same train work, not only its ends.

On start the seeder adds any station or train from this list that is missing (by code or number) and changes nothing that already exists, so a database in use keeps its accounts and bookings. A seeded train deleted in the operations console comes back on the next start; stop bookings on it instead.

This page is generated from the running app (`/api/v1/stations` and `/api/v1/trains`).

## Routes

| Between | Trains (days they leave) |
|---|---|
| Ahmedabad and Delhi | 12957 Swarna Jayanti Rajdhani Express (Daily)<br>12958 Swarna Jayanti Rajdhani Express (Daily) |
| Ahmedabad and Mumbai | 12009 Ahmedabad Shatabdi Express (Not Sun)<br>12010 Mumbai Shatabdi Express (Not Sun) |
| Amritsar and Delhi | 12013 Amritsar Shatabdi Express (Daily)<br>12014 Amritsar New Delhi Shatabdi Express (Daily) |
| Bengaluru and Chennai | 12027 Bengaluru Shatabdi Express (Not Tue)<br>12028 Chennai Shatabdi Express (Not Tue)<br>12639 Brindavan Express (Daily)<br>12640 Brindavan Express (Daily)<br>12657 Chennai Bengaluru Mail (Daily)<br>12658 Bengaluru Chennai Mail (Daily) |
| Bengaluru and Delhi | 12627 Karnataka Express (Daily)<br>12628 Karnataka Express (Daily)<br>22691 KSR Bengaluru Rajdhani Express (Daily)<br>22692 KSR Bengaluru Rajdhani Express (Daily) |
| Bengaluru and Kanniyakumari | 16525 Island Express (Daily)<br>16526 Island Express (Daily) |
| Bengaluru and Kolkata | 12863 Howrah Yesvantpur Superfast Express (Daily)<br>12864 Yesvantpur Howrah Superfast Express (Daily) |
| Bengaluru and Mumbai | 11301 Udyan Express (Daily)<br>11302 Udyan Express (Daily) |
| Bhopal and Delhi | 12001 Bhopal Shatabdi Express (Daily)<br>12002 Bhopal Shatabdi Express (Daily) |
| Chennai and Coimbatore | 12243 Coimbatore Shatabdi Express (Not Tue)<br>12244 Chennai Shatabdi Express (Not Tue)<br>12673 Cheran Superfast Express (Daily)<br>12674 Cheran Superfast Express (Daily)<br>12675 Kovai Express (Daily)<br>12676 Kovai Express (Daily) |
| Chennai and Delhi | 12433 Chennai Rajdhani Express (Fri, Sun)<br>12434 Chennai Rajdhani Express (Wed, Fri)<br>12621 Tamil Nadu Express (Daily)<br>12622 Tamil Nadu Express (Daily) |
| Chennai and Hyderabad | 12759 Charminar Express (Daily)<br>12760 Charminar Express (Daily) |
| Chennai and Kolkata | 12841 Coromandel Express (Daily)<br>12842 Coromandel Express (Daily) |
| Chennai and Mumbai | 22159 Mumbai Chennai Superfast Express (Daily)<br>22160 Chennai Mumbai Superfast Express (Daily) |
| Chennai and Mysuru | 12007 Mysuru Shatabdi Express (Not Wed)<br>12008 Mysuru Chennai Shatabdi Express (Not Wed)<br>20607 Chennai Mysuru Vande Bharat Express (Not Wed)<br>20608 Mysuru Chennai Vande Bharat Express (Not Wed) |
| Chennai and Thiruvananthapuram | 12695 Chennai Thiruvananthapuram Superfast Express (Daily)<br>12696 Thiruvananthapuram Chennai Superfast Express (Daily) |
| Dehradun and Delhi | 12017 Dehradun Shatabdi Express (Daily)<br>12018 Dehradun New Delhi Shatabdi Express (Daily) |
| Delhi and Hyderabad | 12723 Telangana Express (Daily)<br>12724 Telangana Express (Daily) |
| Delhi and Jhansi | 12049 Gatimaan Express (Not Fri)<br>12050 Gatimaan Express (Not Fri) |
| Delhi and Katra | 22439 New Delhi Katra Vande Bharat Express (Not Tue)<br>22440 Katra New Delhi Vande Bharat Express (Not Tue) |
| Delhi and Kolkata | 12301 Howrah Rajdhani Express (Daily)<br>12302 Howrah Rajdhani Express (Daily) |
| Delhi and Lucknow | 12003 Lucknow Shatabdi Express (Daily)<br>12004 Lucknow Shatabdi Express (Daily) |
| Delhi and Mumbai | 12951 Mumbai Rajdhani Express (Daily)<br>12952 Mumbai Rajdhani Express (Daily)<br>12953 August Kranti Rajdhani Express (Daily)<br>12954 August Kranti Rajdhani Express (Daily) |
| Delhi and Patna | 12309 Patna Rajdhani Express (Daily)<br>12310 Patna Rajdhani Express (Daily) |
| Delhi and Varanasi | 22435 New Delhi Varanasi Vande Bharat Express (Not Mon, Thu)<br>22436 Varanasi New Delhi Vande Bharat Express (Not Mon, Thu) |
| Guwahati and Kolkata | 12345 Saraighat Express (Daily)<br>12346 Saraighat Express (Daily) |
| Hyderabad and Mumbai | 12701 Hussainsagar Express (Daily)<br>12702 Hussainsagar Express (Daily) |
| Hyderabad and Mysuru | 12785 Kacheguda Mysuru Superfast Express (Daily)<br>12786 Mysuru Kacheguda Superfast Express (Daily) |
| Hyderabad and Pune | 12025 Pune Secunderabad Shatabdi Express (Not Tue)<br>12026 Secunderabad Pune Shatabdi Express (Not Tue) |
| Kolkata and Mumbai | 12809 Howrah Mail via Nagpur (Daily)<br>12810 Mumbai Mail via Nagpur (Daily) |
| Kolkata and Puri | 12821 Dhauli Express (Daily)<br>12822 Dhauli Express (Daily) |
| Madgaon and Mumbai | 10103 Mandovi Express (Daily)<br>10104 Mandovi Express (Daily)<br>12051 Mumbai Madgaon Jan Shatabdi Express (Daily)<br>12052 Madgaon Mumbai Jan Shatabdi Express (Daily) |
| Mumbai and Pune | 12123 Deccan Queen (Daily)<br>12124 Deccan Queen (Daily)<br>12127 Mumbai Pune Intercity Express (Daily)<br>12128 Pune Mumbai Intercity Express (Daily) |
| Mumbai and Thiruvananthapuram | 16345 Netravati Express (Daily)<br>16346 Netravati Express (Daily) |

## Searches to try

| From | To | Finds |
|---|---|---|
| Chennai Central (`MAS`) | KSR Bengaluru City (`SBC`) | 12007, 12027, 12639, 12657, 20607 |
| Mumbai CSMT (`CSMT`) | Chennai Central (`MAS`) | 22159 |
| Mumbai CSMT (`CSMT`) | Howrah Junction (`HWH`) | 12809 |
| Mumbai Central (`MMCT`) | New Delhi (`NDLS`) | 12951 |
| Mumbai CSMT (`CSMT`) | Pune Junction (`PUNE`) | 11301, 12123, 12127, 12701, 22159 |
| Lokmanya Tilak Terminus (`LTT`) | Ernakulam Junction (`ERS`) | 16345 |
| Howrah Junction (`HWH`) | New Delhi (`NDLS`) | 12301 |
| Ahmedabad Junction (`ADI`) | Jaipur Junction (`JP`) | 12957 |
| New Delhi (`NDLS`) | Amritsar Junction (`ASR`) | 12013 |
| New Delhi (`NDLS`) | Dehradun (`DDN`) | 12017 |
| Howrah Junction (`HWH`) | Guwahati (`GHY`) | 12345 |
| Kacheguda (`KCG`) | Mysuru Junction (`MYS`) | 12785 |
| Dadar (`DR`) | Solapur (`SUR`) | 11301, 12701, 22159 |
| Surat (`ST`) | Kota Junction (`KOTA`) | 12951, 12953 |

## Every train

| Train | Type | Runs | Route | Distance | Journey | Classes |
|---|---|---|---|---|---|---|
| 10103 Mandovi Express | Mail/Express | Daily | CSMT - TNA - PNVL - RN - KUDL - THVM - MAO | 588 km | 11 h 35 min | 2A, 3A, SL, 2S |
| 10104 Mandovi Express | Mail/Express | Daily | MAO - THVM - KUDL - RN - PNVL - TNA - CSMT | 588 km | 11 h 35 min | 2A, 3A, SL, 2S |
| 11301 Udyan Express | Mail/Express | Daily | CSMT - DR - TNA - KYN - LNL - PUNE - DD - SUR - KLBG - WADI - RC - GTL - ATP - DMM - YPR - SBC | 1152 km | 22 h 06 min | 2A, 3A, SL |
| 11302 Udyan Express | Mail/Express | Daily | SBC - YPR - DMM - ATP - GTL - RC - WADI - KLBG - SUR - DD - PUNE - LNL - KYN - TNA - DR - CSMT | 1152 km | 22 h 06 min | 2A, 3A, SL |
| 12001 Bhopal Shatabdi Express | Shatabdi | Daily | RKMP - BPL - JHS - GWL - AGC - MTJ - NDLS | 707 km | 8 h 40 min | CC, EC |
| 12002 Bhopal Shatabdi Express | Shatabdi | Daily | NDLS - MTJ - AGC - GWL - JHS - BPL - RKMP | 707 km | 8 h 40 min | CC, EC |
| 12003 Lucknow Shatabdi Express | Shatabdi | Daily | LKO - CNB - ALJN - GZB - NDLS | 512 km | 5 h 45 min | CC, EC |
| 12004 Lucknow Shatabdi Express | Shatabdi | Daily | NDLS - GZB - ALJN - CNB - LKO | 512 km | 5 h 45 min | CC, EC |
| 12007 Mysuru Shatabdi Express | Shatabdi | Not Wed | MAS - KPD - BNC - SBC - MYA - MYS | 497 km | 6 h 50 min | CC, EC |
| 12008 Mysuru Chennai Shatabdi Express | Shatabdi | Not Wed | MYS - MYA - SBC - BNC - KPD - MAS | 497 km | 6 h 50 min | CC, EC |
| 12009 Ahmedabad Shatabdi Express | Shatabdi | Not Sun | MMCT - BVI - ST - BRC - ADI | 491 km | 6 h 25 min | CC, EC |
| 12010 Mumbai Shatabdi Express | Shatabdi | Not Sun | ADI - BRC - ST - BVI - MMCT | 491 km | 6 h 25 min | CC, EC |
| 12013 Amritsar Shatabdi Express | Shatabdi | Daily | NDLS - UMB - LDH - JUC - ASR | 449 km | 4 h 47 min | CC, EC |
| 12014 Amritsar New Delhi Shatabdi Express | Shatabdi | Daily | ASR - JUC - LDH - UMB - NDLS | 449 km | 4 h 47 min | CC, EC |
| 12017 Dehradun Shatabdi Express | Shatabdi | Daily | NDLS - MTC - MOZ - RK - HW - DDN | 302 km | 4 h 36 min | CC, EC |
| 12018 Dehradun New Delhi Shatabdi Express | Shatabdi | Daily | DDN - HW - RK - MOZ - MTC - NDLS | 302 km | 4 h 36 min | CC, EC |
| 12025 Pune Secunderabad Shatabdi Express | Shatabdi | Not Tue | PUNE - DD - SUR - KLBG - WADI - SC | 607 km | 8 h 30 min | CC, EC |
| 12026 Secunderabad Pune Shatabdi Express | Shatabdi | Not Tue | SC - WADI - KLBG - SUR - DD - PUNE | 607 km | 8 h 30 min | CC, EC |
| 12027 Bengaluru Shatabdi Express | Shatabdi | Not Tue | MAS - KPD - BNC - SBC | 358 km | 5 h 10 min | CC, EC |
| 12028 Chennai Shatabdi Express | Shatabdi | Not Tue | SBC - BNC - KPD - MAS | 358 km | 5 h 10 min | CC, EC |
| 12049 Gatimaan Express | Gatimaan | Not Fri | NZM - AGC - GWL - JHS | 403 km | 4 h 25 min | CC, EC |
| 12050 Gatimaan Express | Gatimaan | Not Fri | JHS - GWL - AGC - NZM | 403 km | 4 h 25 min | CC, EC |
| 12051 Mumbai Madgaon Jan Shatabdi Express | Jan Shatabdi | Daily | CSMT - DR - TNA - PNVL - RN - KUDL - THVM - MAO | 588 km | 9 h 35 min | CC, 2S |
| 12052 Madgaon Mumbai Jan Shatabdi Express | Jan Shatabdi | Daily | MAO - THVM - KUDL - RN - PNVL - TNA - DR - CSMT | 588 km | 9 h 35 min | CC, 2S |
| 12123 Deccan Queen | Superfast | Daily | CSMT - DR - TNA - KYN - LNL - PUNE | 192 km | 3 h 15 min | CC, 2S |
| 12124 Deccan Queen | Superfast | Daily | PUNE - LNL - KYN - TNA - DR - CSMT | 192 km | 3 h 15 min | CC, 2S |
| 12127 Mumbai Pune Intercity Express | Superfast | Daily | CSMT - DR - TNA - KYN - LNL - PUNE | 192 km | 3 h 17 min | CC, 2S |
| 12128 Pune Mumbai Intercity Express | Superfast | Daily | PUNE - LNL - KYN - TNA - DR - CSMT | 192 km | 3 h 17 min | CC, 2S |
| 12243 Coimbatore Shatabdi Express | Shatabdi | Not Tue | MAS - KPD - JTJ - SA - ED - TUP - CBE | 494 km | 6 h 30 min | CC, EC |
| 12244 Chennai Shatabdi Express | Shatabdi | Not Tue | CBE - TUP - ED - SA - JTJ - KPD - MAS | 494 km | 6 h 30 min | CC, EC |
| 12301 Howrah Rajdhani Express | Rajdhani | Daily | HWH - DHN - GAYA - DDU - PRYJ - CNB - NDLS | 1450 km | 17 h 10 min | 1A, 2A, 3A |
| 12302 Howrah Rajdhani Express | Rajdhani | Daily | NDLS - CNB - PRYJ - DDU - GAYA - DHN - HWH | 1450 km | 17 h 10 min | 1A, 2A, 3A |
| 12309 Patna Rajdhani Express | Rajdhani | Daily | RJPB - PNBE - ARA - BXR - DDU - PRYJ - CNB - NDLS | 1005 km | 12 h 50 min | 1A, 2A, 3A |
| 12310 Patna Rajdhani Express | Rajdhani | Daily | NDLS - CNB - PRYJ - DDU - BXR - ARA - PNBE - RJPB | 1005 km | 12 h 50 min | 1A, 2A, 3A |
| 12345 Saraighat Express | Superfast | Daily | HWH - BWN - MLDT - NJP - GHY | 1004 km | 16 h 13 min | 2A, 3A, SL |
| 12346 Saraighat Express | Superfast | Daily | GHY - NJP - MLDT - BWN - HWH | 1004 km | 16 h 13 min | 2A, 3A, SL |
| 12433 Chennai Rajdhani Express | Rajdhani | Fri, Sun | MAS - BZA - WL - BPQ - NGP - BPL - JHS - AGC - NZM | 2176 km | 30 h 05 min | 1A, 2A, 3A |
| 12434 Chennai Rajdhani Express | Rajdhani | Wed, Fri | NZM - AGC - JHS - BPL - NGP - BPQ - WL - BZA - MAS | 2176 km | 30 h 05 min | 1A, 2A, 3A |
| 12621 Tamil Nadu Express | Superfast | Daily | MAS - BZA - WL - BPQ - NGP - ET - BPL - JHS - GWL - AGC - MTJ - NDLS | 2174 km | 33 h 00 min | 1A, 2A, 3A, SL |
| 12622 Tamil Nadu Express | Superfast | Daily | NDLS - MTJ - AGC - GWL - JHS - BPL - ET - NGP - BPQ - WL - BZA - MAS | 2174 km | 33 h 00 min | 1A, 2A, 3A, SL |
| 12627 Karnataka Express | Superfast | Daily | SBC - YPR - DMM - ATP - GTL - RC - WADI - SUR - DD - MMR - BSL - ET - BPL - JHS - GWL - AGC - NDLS | 2385 km | 39 h 55 min | 1A, 2A, 3A, SL |
| 12628 Karnataka Express | Superfast | Daily | NDLS - AGC - GWL - JHS - BPL - ET - BSL - MMR - DD - SUR - WADI - RC - GTL - ATP - DMM - YPR - SBC | 2385 km | 39 h 55 min | 1A, 2A, 3A, SL |
| 12639 Brindavan Express | Superfast | Daily | MAS - AJJ - KPD - JTJ - BWT - KJM - BNC - SBC | 358 km | 6 h 00 min | CC, 2S |
| 12640 Brindavan Express | Superfast | Daily | SBC - BNC - KJM - BWT - JTJ - KPD - AJJ - MAS | 358 km | 6 h 00 min | CC, 2S |
| 12657 Chennai Bengaluru Mail | Mail/Express | Daily | MAS - AJJ - KPD - JTJ - BWT - KJM - BNC - SBC | 358 km | 6 h 15 min | 1A, 2A, 3A, SL |
| 12658 Bengaluru Chennai Mail | Mail/Express | Daily | SBC - BNC - KJM - BWT - JTJ - KPD - AJJ - MAS | 358 km | 6 h 15 min | 1A, 2A, 3A, SL |
| 12673 Cheran Superfast Express | Superfast | Daily | MAS - AJJ - KPD - JTJ - SA - ED - TUP - CBE | 494 km | 8 h 15 min | 2A, 3A, SL |
| 12674 Cheran Superfast Express | Superfast | Daily | CBE - TUP - ED - SA - JTJ - KPD - AJJ - MAS | 494 km | 8 h 15 min | 2A, 3A, SL |
| 12675 Kovai Express | Superfast | Daily | MAS - AJJ - KPD - JTJ - SA - ED - TUP - CBE | 494 km | 7 h 50 min | CC, 2S |
| 12676 Kovai Express | Superfast | Daily | CBE - TUP - ED - SA - JTJ - KPD - AJJ - MAS | 494 km | 7 h 50 min | CC, 2S |
| 12695 Chennai Thiruvananthapuram Superfast Express | Superfast | Daily | MAS - AJJ - KPD - JTJ - SA - ED - TUP - CBE - PGT - TCR - ERS - KTYM - QLN - TVC | 925 km | 16 h 50 min | 2A, 3A, SL |
| 12696 Thiruvananthapuram Chennai Superfast Express | Superfast | Daily | TVC - QLN - KTYM - ERS - TCR - PGT - CBE - TUP - ED - SA - JTJ - KPD - AJJ - MAS | 925 km | 16 h 50 min | 2A, 3A, SL |
| 12701 Hussainsagar Express | Superfast | Daily | CSMT - DR - TNA - KYN - LNL - PUNE - DD - SUR - KLBG - WADI - SC - HYB | 808 km | 14 h 17 min | 2A, 3A, SL |
| 12702 Hussainsagar Express | Superfast | Daily | HYB - SC - WADI - KLBG - SUR - DD - PUNE - LNL - KYN - TNA - DR - CSMT | 808 km | 14 h 17 min | 2A, 3A, SL |
| 12723 Telangana Express | Superfast | Daily | HYB - SC - KZJ - BPQ - NGP - ET - BPL - JHS - GWL - AGC - NDLS | 1676 km | 26 h 15 min | 2A, 3A, SL |
| 12724 Telangana Express | Superfast | Daily | NDLS - AGC - GWL - JHS - BPL - ET - NGP - BPQ - KZJ - SC - HYB | 1676 km | 26 h 15 min | 2A, 3A, SL |
| 12759 Charminar Express | Superfast | Daily | MAS - GDR - NLR - OGL - GNT - NLDA - SC - HYB | 721 km | 13 h 30 min | 1A, 2A, 3A, SL |
| 12760 Charminar Express | Superfast | Daily | HYB - SC - NLDA - GNT - OGL - NLR - GDR - MAS | 721 km | 13 h 30 min | 1A, 2A, 3A, SL |
| 12785 Kacheguda Mysuru Superfast Express | Superfast | Daily | KCG - MBNR - KRNT - DHNE - ATP - DMM - YPR - SBC - MYA - MYS | 752 km | 13 h 53 min | 2A, 3A, SL |
| 12786 Mysuru Kacheguda Superfast Express | Superfast | Daily | MYS - MYA - SBC - YPR - DMM - ATP - DHNE - KRNT - MBNR - KCG | 752 km | 13 h 53 min | 2A, 3A, SL |
| 12809 Howrah Mail via Nagpur | Mail/Express | Daily | CSMT - DR - TNA - KYN - NK - MMR - BSL - AK - NGP - DURG - BSP - JSG - TATA - KGP - HWH | 1968 km | 34 h 57 min | 1A, 2A, 3A, SL |
| 12810 Mumbai Mail via Nagpur | Mail/Express | Daily | HWH - KGP - TATA - JSG - BSP - DURG - NGP - AK - BSL - MMR - NK - KYN - TNA - DR - CSMT | 1968 km | 34 h 57 min | 1A, 2A, 3A, SL |
| 12821 Dhauli Express | Superfast | Daily | HWH - KGP - BLS - CTC - BBS - PURI | 499 km | 7 h 30 min | CC, 2S |
| 12822 Dhauli Express | Superfast | Daily | PURI - BBS - CTC - BLS - KGP - HWH | 499 km | 7 h 30 min | CC, 2S |
| 12841 Coromandel Express | Superfast | Daily | HWH - KGP - BLS - CTC - BBS - BAM - VSKP - RJY - BZA - OGL - NLR - GDR - MAS | 1662 km | 26 h 00 min | 2A, 3A, SL, 2S |
| 12842 Coromandel Express | Superfast | Daily | MAS - GDR - NLR - OGL - BZA - RJY - VSKP - BAM - BBS - CTC - BLS - KGP - HWH | 1662 km | 26 h 00 min | 2A, 3A, SL, 2S |
| 12863 Howrah Yesvantpur Superfast Express | Superfast | Daily | HWH - KGP - BLS - CTC - BBS - BAM - VSKP - RJY - BZA - OGL - NLR - GDR - RU - KPD - JTJ - BWT - KJM - YPR | 1946 km | 31 h 58 min | 2A, 3A, SL |
| 12864 Yesvantpur Howrah Superfast Express | Superfast | Daily | YPR - KJM - BWT - JTJ - KPD - RU - GDR - NLR - OGL - BZA - RJY - VSKP - BAM - BBS - CTC - BLS - KGP - HWH | 1946 km | 31 h 58 min | 2A, 3A, SL |
| 12951 Mumbai Rajdhani Express | Rajdhani | Daily | MMCT - BVI - ST - BRC - RTM - KOTA - NDLS | 1386 km | 15 h 32 min | 1A, 2A, 3A |
| 12952 Mumbai Rajdhani Express | Rajdhani | Daily | NDLS - KOTA - RTM - BRC - ST - BVI - MMCT | 1386 km | 15 h 32 min | 1A, 2A, 3A |
| 12953 August Kranti Rajdhani Express | Rajdhani | Daily | MMCT - BVI - ST - BRC - RTM - KOTA - MTJ - NZM | 1377 km | 17 h 15 min | 1A, 2A, 3A |
| 12954 August Kranti Rajdhani Express | Rajdhani | Daily | NZM - MTJ - KOTA - RTM - BRC - ST - BVI - MMCT | 1377 km | 17 h 15 min | 1A, 2A, 3A |
| 12957 Swarna Jayanti Rajdhani Express | Rajdhani | Daily | ADI - MSH - PNU - ABR - AII - JP - AWR - GGN - NDLS | 934 km | 14 h 33 min | 1A, 2A, 3A |
| 12958 Swarna Jayanti Rajdhani Express | Rajdhani | Daily | NDLS - GGN - AWR - JP - AII - ABR - PNU - MSH - ADI | 934 km | 14 h 33 min | 1A, 2A, 3A |
| 16345 Netravati Express | Mail/Express | Daily | LTT - TNA - PNVL - RN - KUDL - THVM - MAO - KAWR - UD - MAQ - CAN - CLT - SRR - TCR - ERS - QLN - TVC | 1565 km | 30 h 44 min | 2A, 3A, SL |
| 16346 Netravati Express | Mail/Express | Daily | TVC - QLN - ERS - TCR - SRR - CLT - CAN - MAQ - UD - KAWR - MAO - THVM - KUDL - RN - PNVL - TNA - LTT | 1565 km | 30 h 44 min | 2A, 3A, SL |
| 16525 Island Express | Mail/Express | Daily | CAPE - TVC - QLN - KTYM - ERS - TCR - PGT - CBE - TUP - ED - SA - JTJ - BWT - KJM - BNC - SBC | 943 km | 20 h 00 min | 2A, 3A, SL, 2S |
| 16526 Island Express | Mail/Express | Daily | SBC - BNC - KJM - BWT - JTJ - SA - ED - TUP - CBE - PGT - TCR - ERS - KTYM - QLN - TVC - CAPE | 943 km | 20 h 00 min | 2A, 3A, SL, 2S |
| 20607 Chennai Mysuru Vande Bharat Express | Vande Bharat | Not Wed | MAS - KPD - SBC - MYS | 497 km | 6 h 30 min | CC, EC |
| 20608 Mysuru Chennai Vande Bharat Express | Vande Bharat | Not Wed | MYS - SBC - KPD - MAS | 497 km | 6 h 30 min | CC, EC |
| 22159 Mumbai Chennai Superfast Express | Superfast | Daily | CSMT - DR - TNA - KYN - LNL - PUNE - DD - SUR - KLBG - WADI - RC - GTL - RU - AJJ - MAS | 1286 km | 22 h 56 min | 2A, 3A, SL |
| 22160 Chennai Mumbai Superfast Express | Superfast | Daily | MAS - AJJ - RU - GTL - RC - WADI - KLBG - SUR - DD - PUNE - LNL - KYN - TNA - DR - CSMT | 1286 km | 22 h 56 min | 2A, 3A, SL |
| 22435 New Delhi Varanasi Vande Bharat Express | Vande Bharat | Not Mon, Thu | NDLS - CNB - PRYJ - BSB | 759 km | 8 h 00 min | CC, EC |
| 22436 Varanasi New Delhi Vande Bharat Express | Vande Bharat | Not Mon, Thu | BSB - PRYJ - CNB - NDLS | 759 km | 8 h 00 min | CC, EC |
| 22439 New Delhi Katra Vande Bharat Express | Vande Bharat | Not Tue | NDLS - UMB - LDH - JAT - SVDK | 655 km | 8 h 00 min | CC, EC |
| 22440 Katra New Delhi Vande Bharat Express | Vande Bharat | Not Tue | SVDK - JAT - LDH - UMB - NDLS | 655 km | 8 h 00 min | CC, EC |
| 22691 KSR Bengaluru Rajdhani Express | Rajdhani | Daily | SBC - YPR - DMM - ATP - KRNT - SC - KZJ - BPQ - NGP - BPL - JHS - AGC - NZM | 2269 km | 33 h 20 min | 1A, 2A, 3A |
| 22692 KSR Bengaluru Rajdhani Express | Rajdhani | Daily | NZM - AGC - JHS - BPL - NGP - BPQ - KZJ - SC - KRNT - ATP - DMM - YPR - SBC | 2269 km | 33 h 20 min | 1A, 2A, 3A |

## Stations

Grouped by state. The number is how many trains call there.

| State | Stations |
|---|---|
| Andhra Pradesh | Anantapur `ATP` (8); Dharmavaram Junction `DMM` (8); Dhone Junction `DHNE` (2); Gudur Junction `GDR` (6); Guntakal Junction `GTL` (6); Guntur Junction `GNT` (2); Kurnool City `KRNT` (4); Nellore `NLR` (6); Ongole `OGL` (6); Rajahmundry `RJY` (4); Renigunta Junction `RU` (4); Vijayawada Junction `BZA` (8); Visakhapatnam `VSKP` (4) |
| Assam | Guwahati `GHY` (2) |
| Bihar | Ara Junction `ARA` (2); Buxar `BXR` (2); Gaya Junction `GAYA` (2); Patna Junction `PNBE` (2); Rajendra Nagar Terminal `RJPB` (2) |
| Chhattisgarh | Bilaspur Junction `BSP` (2); Durg Junction `DURG` (2) |
| Delhi | Hazrat Nizamuddin `NZM` (8); New Delhi `NDLS` (26) |
| Goa | Madgaon Junction `MAO` (6); Thivim `THVM` (6) |
| Gujarat | Ahmedabad Junction `ADI` (4); Mahesana Junction `MSH` (2); Palanpur Junction `PNU` (2); Surat `ST` (6); Vadodara Junction `BRC` (6) |
| Haryana | Ambala Cantt `UMB` (4); Gurugram `GGN` (2) |
| Jammu and Kashmir | Jammu Tawi `JAT` (2); Shri Mata Vaishno Devi Katra `SVDK` (2) |
| Jharkhand | Dhanbad Junction `DHN` (2); Tatanagar Junction `TATA` (2) |
| Karnataka | Bangarapet Junction `BWT` (8); Bengaluru Cantonment `BNC` (10); KSR Bengaluru City `SBC` (20); Krishnarajapuram `KJM` (8); Yesvantpur Junction `YPR` (10); Kalaburagi `KLBG` (8); Karwar `KAWR` (2); Mandya `MYA` (4); Mangaluru Central `MAQ` (2); Mysuru Junction `MYS` (6); Raichur `RC` (6); Udupi `UD` (2); Wadi Junction `WADI` (10) |
| Kerala | Kannur `CAN` (2); Ernakulam Junction `ERS` (6); Kollam Junction `QLN` (6); Kottayam `KTYM` (4); Kozhikode `CLT` (2); Palakkad Junction `PGT` (4); Shoranur Junction `SRR` (2); Thiruvananthapuram Central `TVC` (6); Thrissur `TCR` (6) |
| Madhya Pradesh | Bhopal Junction `BPL` (12); Rani Kamlapati `RKMP` (2); Gwalior Junction `GWL` (10); Itarsi Junction `ET` (6); Ratlam Junction `RTM` (4) |
| Maharashtra | Akola Junction `AK` (2); Balharshah `BPQ` (8); Bhusaval Junction `BSL` (4); Daund Junction `DD` (10); Kalyan Junction `KYN` (12); Kudal `KUDL` (6); Lonavala `LNL` (10); Manmad Junction `MMR` (4); Borivali `BVI` (6); Dadar `DR` (14); Lokmanya Tilak Terminus `LTT` (2); Mumbai CSMT `CSMT` (16); Mumbai Central `MMCT` (6); Nagpur Junction `NGP` (10); Nashik Road `NK` (2); Panvel `PNVL` (6); Pune Junction `PUNE` (12); Ratnagiri `RN` (6); Solapur `SUR` (10); Thane `TNA` (18) |
| Odisha | Balasore `BLS` (6); Bhubaneswar `BBS` (6); Brahmapur `BAM` (4); Cuttack `CTC` (6); Jharsuguda Junction `JSG` (2); Puri `PURI` (2) |
| Punjab | Amritsar Junction `ASR` (2); Jalandhar City `JUC` (2); Ludhiana Junction `LDH` (4) |
| Rajasthan | Abu Road `ABR` (2); Ajmer Junction `AII` (2); Alwar Junction `AWR` (2); Jaipur Junction `JP` (2); Kota Junction `KOTA` (4) |
| Tamil Nadu | Arakkonam Junction `AJJ` (12); Chennai Central `MAS` (28); Coimbatore Junction `CBE` (10); Erode Junction `ED` (10); Jolarpettai Junction `JTJ` (16); Kanniyakumari `CAPE` (2); Salem Junction `SA` (10); Tiruppur `TUP` (10); Katpadi Junction `KPD` (20) |
| Telangana | Hyderabad Deccan `HYB` (6); Kacheguda `KCG` (2); Secunderabad Junction `SC` (10); Mahbubnagar `MBNR` (2); Nalgonda `NLDA` (2); Kazipet Junction `KZJ` (4); Warangal `WL` (4) |
| Uttar Pradesh | Agra Cantt `AGC` (14); Aligarh Junction `ALJN` (2); Pt. Deen Dayal Upadhyaya Junction `DDU` (4); Ghaziabad `GZB` (2); Virangana Lakshmibai Jhansi `JHS` (14); Kanpur Central `CNB` (8); Lucknow Charbagh `LKO` (2); Mathura Junction `MTJ` (6); Meerut City `MTC` (2); Muzaffarnagar `MOZ` (2); Prayagraj Junction `PRYJ` (6); Varanasi Junction `BSB` (2) |
| Uttarakhand | Dehradun `DDN` (2); Haridwar Junction `HW` (2); Roorkee `RK` (2) |
| West Bengal | Barddhaman Junction `BWN` (2); Kharagpur Junction `KGP` (8); Howrah Junction `HWH` (12); Malda Town `MLDT` (2); New Jalpaiguri `NJP` (2) |

Class codes: `1A` First AC, `2A` AC 2 Tier, `3A` AC 3 Tier, `SL` Sleeper, `CC` AC Chair Car, `EC` Executive Chair Car, `2S` Second Sitting.
