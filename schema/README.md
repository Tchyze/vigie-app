# Copie du contrat de données

Ces fichiers sont une **copie** des JSON Schema de
[`vigie-collector`](https://github.com/Tchyze/vigie-collector) (`schema/`), qui en est la source de
vérité. La CI s'en sert pour vérifier les JSON d'exemple de `app/src/main/assets/demo/`.

Après une évolution du format côté collecteur, recopiez-les :

```bash
cp ../vigie-collector/schema/*.schema.json schema/
```
