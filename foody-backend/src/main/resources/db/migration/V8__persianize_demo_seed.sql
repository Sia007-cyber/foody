-- V8: The app's UI is fully Persian; the English demo seed data from V2 stood out
-- awkwardly during manual testing. Update (not re-insert) the same rows to Persian
-- copy so demo/dev environments look consistent with production content.

UPDATE business_types SET label = 'کافه' WHERE code = 'CAFE';
UPDATE business_types SET label = 'فست‌فود' WHERE code = 'FAST_FOOD';

UPDATE businesses
SET name = 'کافه سان‌رایز',
    description = 'یک کافه دنج محله‌ای با قهوه و شیرینی تازه.',
    address = 'خیابان بهارستان، پلاک ۱۲۳'
WHERE id = 1;

UPDATE menus SET name = 'منوی اصلی' WHERE id = 1;

UPDATE products SET name = 'اسپرسو', description = 'یک شات اسپرسوی غلیظ.' WHERE id = 1;
UPDATE products SET name = 'کاپوچینو', description = 'اسپرسو با فوم شیر بخارداده.' WHERE id = 2;
UPDATE products SET name = 'کروسان', description = 'شیرینی لایه‌ای و کره‌ای.' WHERE id = 3;
UPDATE products SET name = 'لاته', description = 'اسپرسو با مقدار زیادی شیر بخارداده.' WHERE id = 4;
