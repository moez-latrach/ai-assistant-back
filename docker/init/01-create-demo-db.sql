-- Exécuté par PostgreSQL uniquement à la création du volume (base vide).
-- Base séparée du profil "azure" : ne contient que des documents publics de démonstration.
-- Les documents locaux / confidentiels restent dans "ragdb" et ne sont jamais accessibles en mode azure.
CREATE DATABASE ragdb_demo OWNER userapp;
