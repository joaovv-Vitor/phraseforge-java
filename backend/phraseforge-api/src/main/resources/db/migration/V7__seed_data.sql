-- Authors
INSERT INTO authors (name, slug, birth_year, death_year, biography, created_at, updated_at)
VALUES
('Marcus Aurelius', 'marcus-aurelius', 121, 180, 'Roman emperor and Stoic philosopher. His private notes, the Meditations, are a landmark of Stoic thought.', NOW(), NOW()),
('Socrates', 'socrates', -470, -399, 'Athenian philosopher credited as a founder of Western philosophy. Known for the Socratic method and his trial and execution.', NOW(), NOW()),
('Aristotle', 'aristotle', -384, -322, 'Greek philosopher and polymath, student of Plato and tutor of Alexander the Great. Founder of the Peripatetic school.', NOW(), NOW()),
('Friedrich Nietzsche', 'friedrich-nietzsche', 1844, 1900, 'German philosopher and cultural critic whose work influenced modern existentialism and postmodern thought.', NOW(), NOW()),
('Simone de Beauvoir', 'simone-de-beauvoir', 1908, 1986, 'French existentialist philosopher, writer, and feminist theorist, author of The Second Sex.', NOW(), NOW()),
('Albert Einstein', 'albert-einstein', 1879, 1955, 'German-born theoretical physicist who developed the theory of relativity. Nobel laureate in Physics 1921.', NOW(), NOW()),
('Virginia Woolf', 'virginia-woolf', 1882, 1941, 'English modernist writer, pioneer of the stream-of-consciousness narrative.', NOW(), NOW()),
('Epictetus', 'epictetus', NULL, NULL, 'Greek Stoic philosopher, born a slave. His teachings were recorded by his pupil Arrian in the Discourses and the Enchiridion. Exact years are uncertain, so none are recorded.', NOW(), NOW());

-- Categories
INSERT INTO categories (name, slug, description, created_at, updated_at)
VALUES
('Filosofia', 'filosofia', 'Pensamento filosófico de diversas tradições.', NOW(), NOW()),
('Estoicismo', 'estoicismo', 'Filosofia prática greco-romana centrada na virtude e no controle das emoções.', NOW(), NOW()),
('Existencialismo', 'existencialismo', 'Filosofia centrada na liberdade, na escolha e no sentido da existência.', NOW(), NOW()),
('Ciência', 'ciencia', 'Frase sobre ciência, conhecimento e o universo.', NOW(), NOW()),
('Literatura', 'literatura', 'Frase de obras literárias e escritores.', NOW(), NOW()),
('Motivação', 'motivacao', 'Frases que inspiram ação e superação.', NOW(), NOW()),
('Sabedoria', 'sabedoria', 'Frases de sabedoria prática e reflexões sobre a vida.', NOW(), NOW());

-- Tags
INSERT INTO tags (name, created_at, updated_at)
VALUES
('mente', NOW(), NOW()),
('força', NOW(), NOW()),
('controle', NOW(), NOW()),
('resiliência', NOW(), NOW()),
('sabedoria', NOW(), NOW()),
('ação', NOW(), NOW()),
('liberdade', NOW(), NOW()),
('mudança', NOW(), NOW()),
('identidade', NOW(), NOW()),
('conhecimento', NOW(), NOW()),
('verdade', NOW(), NOW()),
('coragem', NOW(), NOW()),
('tempo', NOW(), NOW()),
('paz', NOW(), NOW());

-- Phrases (year intentionally NULL where dating is uncertain — MVP does not model approximate dates)
INSERT INTO phrases (content, author_id, year, language, source, created_at, updated_at)
VALUES
('Você tem poder sobre sua mente, não sobre os eventos externos. Perceba isso e encontrará força.',
 (SELECT id FROM authors WHERE slug = 'marcus-aurelius'), 170, 'pt', 'Meditações', NOW(), NOW()),
('Aquilo que se interpõe no caminho torna-se o caminho.',
 (SELECT id FROM authors WHERE slug = 'marcus-aurelius'), NULL, 'pt', 'Meditações', NOW(), NOW()),
('Conhece-te a ti mesmo.',
 (SELECT id FROM authors WHERE slug = 'socrates'), NULL, 'pt', NULL, NOW(), NOW()),
('A vida não examinada não vale a pena ser vivida.',
 (SELECT id FROM authors WHERE slug = 'socrates'), NULL, 'pt', NULL, NOW(), NOW()),
('Somos o que fazemos repetidamente. A excelência, então, não é um ato, mas um hábito.',
 (SELECT id FROM authors WHERE slug = 'aristotle'), NULL, 'pt', NULL, NOW(), NOW()),
('O que não nos mata nos torna mais fortes.',
 (SELECT id FROM authors WHERE slug = 'friedrich-nietzsche'), 1888, 'pt', 'Crepúsculo dos Ídolos', NOW(), NOW()),
('Quem tem um porquê para viver pode suportar quase qualquer como.',
 (SELECT id FROM authors WHERE slug = 'friedrich-nietzsche'), NULL, 'pt', NULL, NOW(), NOW()),
('Não se nasce mulher, torna-se mulher.',
 (SELECT id FROM authors WHERE slug = 'simone-de-beauvoir'), 1949, 'pt', 'O Segundo Sexo', NOW(), NOW()),
('A imaginação é mais importante que o conhecimento.',
 (SELECT id FROM authors WHERE slug = 'albert-einstein'), NULL, 'pt', NULL, NOW(), NOW()),
('A lógica levará você de A a B. A imaginação levará você a qualquer lugar.',
 (SELECT id FROM authors WHERE slug = 'albert-einstein'), NULL, 'en', NULL, NOW(), NOW()),
('Não se encontra paz evitando a vida.',
 (SELECT id FROM authors WHERE slug = 'virginia-woolf'), NULL, 'pt', NULL, NOW(), NOW()),
('No fundo do inverno, aprendi enfim que dentro de mim havia um verão invencível.',
 (SELECT id FROM authors WHERE slug = 'virginia-woolf'), NULL, 'pt', NULL, NOW(), NOW()),
('Não são os acontecimentos que perturbam as pessoas, mas sim seus julgamentos a respeito deles.',
 (SELECT id FROM authors WHERE slug = 'epictetus'), NULL, 'pt', 'Enchiridion', NOW(), NOW()),
('Apenas os instruídos são livres.',
 (SELECT id FROM authors WHERE slug = 'epictetus'), NULL, 'pt', 'Discursos', NOW(), NOW());

-- Phrase <-> Category links
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'estoicismo' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'marcus-aurelius');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'filosofia' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'socrates');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'filosofia' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'aristotle');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'filosofia' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'friedrich-nietzsche');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'existencialismo' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'simone-de-beauvoir');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'ciencia' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'albert-einstein');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'literatura' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'virginia-woolf');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'estoicismo' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'epictetus');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'sabedoria' WHERE p.author_id IN (SELECT id FROM authors WHERE slug IN ('epictetus', 'socrates'));

-- Phrase <-> Tag links
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'mente' WHERE p.content LIKE 'Você tem poder%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'resiliência' WHERE p.content LIKE 'Aquilo que se interpõe%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'sabedoria' WHERE p.content LIKE 'Conhece-te%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'sabedoria' WHERE p.content LIKE 'A vida não examinada%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'ação' WHERE p.content LIKE 'Somos o que fazemos%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'força' WHERE p.content LIKE 'O que não nos mata%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'identidade' WHERE p.content LIKE 'Não se nasce mulher%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'conhecimento' WHERE p.content LIKE 'A imaginação é mais%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'paz' WHERE p.content LIKE 'Não se encontra paz%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'coragem' WHERE p.content LIKE 'No fundo do inverno%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'controle' WHERE p.content LIKE 'Não são os acontecimentos%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'liberdade' WHERE p.content LIKE 'Apenas os instruídos%';
