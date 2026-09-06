import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("../src/features/reservations/NewReservationPage.tsx", import.meta.url), "utf8");

test("successful reservation creation returns customers to the existing reservations page", () => {
  assert.match(source, /navigate\("\/reservations"\)/);
  assert.doesNotMatch(source, /navigate\(`\/reservations\/\$\{reservation\.id\}`\)/);
});
