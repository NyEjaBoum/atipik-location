<template>
  <div class="content-wrapper">
    <h1>Fiche de la réservation</h1>
    <div v-if="fiche">
      <div class="box-fiche">
        <div><b>Client :</b> {{ fiche.client }}</div>
        <div><b>Date :</b> {{ fiche.date }}</div>
        <div><b>Remarque :</b> {{ fiche.remarque }}</div>
        <div><b>État :</b> {{ fiche.etat }}</div>
        <div><b>Montant :</b> {{ fiche.montant }}</div>
        <div><b>Montant caution :</b> {{ fiche.montantCaution }}</div>
        <div><b>Reste à payer :</b> {{ fiche.resteAPayer }}</div>
      </div>
      <h3>Détails</h3>
      <table class="apj-list-table">
        <thead>
          <tr>
            <th>Produit</th>
            <th>Référence</th>
            <th>Image</th>
            <th>Quantité (Article)</th>
            <th>Quantité (En Jour)</th>
            <th>Date</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in fiche.details" :key="d.id">
            <td>{{ d.produit }}</td>
            <td>{{ d.reference }}</td>
            <td><img :src="d.image" alt="" width="60" v-if="d.image"></td>
            <td>{{ d.qtearticle }}</td>
            <td>{{ d.qte }}</td>
            <td>{{ d.date }}</td>
          </tr>
        </tbody>
      </table>
    <router-link
        v-if="fiche.cautionId"
        class="apj-btn"
        :to="`/caisse/mvt-caisse-saisie-caution?idcaution=${fiche.cautionId}&idreservation=${fiche.id}&type=regler`"
      >
        Régler caution
      </router-link>
    </div>
    <div v-else>Chargement...</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { fetchReservationFiche } from '@/services/services.js'

const fiche = ref(null)
const route = useRoute()

onMounted(async () => {
  fiche.value = await fetchReservationFiche(route.params.id)
})
</script>