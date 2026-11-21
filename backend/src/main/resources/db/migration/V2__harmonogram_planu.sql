-- Harmonogram wydawania planu przydziału (wynik algorytmu przydziału): który budynek w którym oknie czasowym
-- odbiera zasoby w danym punkcie wydawania. Ekran „Plan przydziału” pokazuje go obok pozycji planu.

CREATE TABLE plan_harmonogram (
    id         BIGSERIAL PRIMARY KEY,
    plan_id    BIGINT NOT NULL REFERENCES plan_przydzialu (id) ON DELETE CASCADE,
    punkt_id   INTEGER NOT NULL REFERENCES punkt_wydawania (id),
    okno       TEXT NOT NULL,                       -- np. 08:00-10:00
    budynek_id INTEGER NOT NULL REFERENCES budynek (id)
);
CREATE INDEX idx_plan_harmonogram_plan ON plan_harmonogram (plan_id);
