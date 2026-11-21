package pl.ans.ipbisr.domain;

import java.util.List;

/** Punkt wydawania zasobów: przepustowość transportu (kg) i budynki, które z niego korzystają. */
public record PunktWydawania(int id, double przepustowoscKg, List<Integer> budynki) {}
