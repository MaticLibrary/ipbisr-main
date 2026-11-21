package pl.ans.ipbisr.domain;

/**
 * Grupa mieszkańców o takich samych potrzebach: ten sam budynek, priorytet, poziom potrzeb i grupa wiekowa
 * (DZIECKO / DOROSLY / SENIOR). Dla jednej grupy wystarczy raz wywołać tabelę DMN „Normy dzienne”.
 */
public record GrupaPotrzeb(int budynekId, String priorytet, String poziomPotrzeb, String grupaWiekowa, int liczba) {}
