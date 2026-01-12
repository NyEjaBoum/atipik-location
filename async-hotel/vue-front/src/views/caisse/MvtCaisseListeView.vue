<template>
  <div class="content-wrapper">
    <section class="content-header">
      <h1>Liste des mouvements de caisse</h1>
    </section>
    <section class="content">
      <form @submit.prevent="fetchData">
        <div class="filters">
          <label>Désignation</label>
          <input v-model="designation" type="text" class="form-control" />

          <label>Date min</label>
          <input v-model="daty1" type="date" class="form-control" />

          <label>Date max</label>
          <input v-model="daty2" type="date" class="form-control" />

          <label>Voir l'état</label>
          <select v-model="etat" class="form-control">
            <option value="">Tous</option>
            <option value="_cree">Créé(e)</option>
            <option value="_valider">Visé(e)</option>
          </select>
          <button type="submit" class="btn btn-primary btn-small">Consultez</button>
        </div>
      </form>
      <table class="table table-bordered">
        <thead>
          <tr>
            <th>Id</th>
            <th>Date</th>
            <th>Désignation</th>
            <th>Caisse</th>
            <th>Détail de vente</th>
            <th>Virement</th>
            <th>Crédit</th>
            <th>Débit</th>
            <th>État</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="mvt in mouvements" :key="mvt.id">
            <td>{{ mvt.id }}</td>
            <td>{{ mvt.daty }}</td>
            <td>{{ mvt.designation }}</td>
            <td>{{ mvt.idCaisseLib }}</td>
            <td>{{ mvt.idVenteDetail }}</td>
            <td>{{ mvt.idVirement }}</td>
            <td>{{ mvt.credit }}</td>
            <td>{{ mvt.debit }}</td>
            <td>{{ mvt.etatLib }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script>
import { fetchMvtCaisseListe } from '@/services/services.js'

const today = () => {
  const d = new Date();
  return d.toISOString().slice(0, 10);
};
export default {
  name: "MvtCaisseListeView",
  data() {
    return {
      mouvements: [],
      etat: "",
      designation: "",
      daty1: today(),
      daty2: today()
    };
  },
  methods: {
    async fetchData() {
      try {
        this.mouvements = await fetchMvtCaisseListe({
          etat: this.etat,
          designation: this.designation,
          daty1: this.daty1,
          daty2: this.daty2
        });
      } catch (e) {
        console.error('Erreur:', e);
        this.mouvements = [];
      }
    }
  },
  mounted() {
    this.fetchData();
  }
};
</script>