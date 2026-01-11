<template>
  <div class="content-wrapper">
    <h1>Saisie de mouvement de caisse</h1>
    <form @submit.prevent="submitForm">
      <div class="row">
        <div class="col-md-6">
          <label>Type d'opération</label>
          <select v-model="form.type" class="form-control" @change="reloadForm">
            <option value="encaisser">Encaisser la caution</option>
            <option value="regler">Rembourser la caution</option>
          </select>
        </div>
        <div class="col-md-6"></div>
      </div>
      <div class="row">
        <div class="col-md-6">
          <label>Date</label>
          <input type="date" v-model="form.daty" class="form-control" required />
        </div>
        <div class="col-md-6">
          <label>Désignation</label>
          <input type="text" v-model="form.designation" class="form-control" />
        </div>
      </div>
      <div class="row">
        <div class="col-md-6">
          <label>Caisse</label>
          <select v-model="form.idCaisse" class="form-control" required>
            <option v-for="caisse in caisses" :key="caisse.id" :value="caisse.id">
              {{ caisse.val }}
            </option>
          </select>
        </div>
        <div class="col-md-6">
          <label>Tiers</label>
          <input type="text" v-model="form.tiers" class="form-control" readonly />
        </div>
      </div>
      <div class="row">
        <div class="col-md-6">
          <label>Débit</label>
          <input type="number" v-model="form.debit" class="form-control" readonly />
        </div>
        <div class="col-md-6">
          <label>Crédit</label>
          <input type="number" v-model="form.credit" class="form-control" readonly />
        </div>
      </div>
      <div class="row mt-3">
        <div class="col-md-6">
          <button type="reset" class="btn btn-secondary">Réinitialiser</button>
        </div>
        <div class="col-md-6 text-right">
          <button type="submit" class="btn btn-primary">Enregistrer</button>
        </div>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

function todayISO() {
  const d = new Date()
  return d.toISOString().slice(0, 10)
}

const form = ref({
  daty: todayISO(),
  designation: '',
  idCaisse: '',
  tiers: '', // affichage uniquement
  debit: 0,
  credit: 0,
  idcaution: '',
  idreservation: '',
  type: 'encaisser' // valeur par défaut
})
const caisses = ref([])

async function reloadForm() {
  // Recharge les infos du formulaire selon le type choisi
  const resp = await fetch(`/asynclocation/api/caisse/formulaire-mvt-caution?idcaution=${form.value.idcaution}&idreservation=${form.value.idreservation}&type=${form.value.type}`)
  const data = await resp.json()
  Object.assign(form.value, data)
  if (!form.value.daty) {
    form.value.daty = todayISO()
  }
}

onMounted(async () => {
  form.value.idcaution = route.query.idcaution || ''
  form.value.idreservation = route.query.idreservation || ''
  form.value.type = route.query.type || 'encaisser'

  await reloadForm()

  // Charger la liste des caisses
  const respCaisses = await fetch('/asynclocation/api/caisse/liste')
  caisses.value = await respCaisses.json()
})

async function submitForm() {
  try {
    let daty = form.value.daty
    if (daty && daty.includes('/')) {
      const [day, month, year] = daty.split('/')
      daty = `${year}-${month}-${day}`
    }

    const payload = {
      daty: daty,
      designation: form.value.designation,
      idCaisse: form.value.idCaisse,
      debit: form.value.debit,
      credit: form.value.credit,
      type: form.value.type,
      idcaution: form.value.idcaution,
      idreservation: form.value.idreservation
    }

    const resp = await fetch('/asynclocation/api/caisse/mvt-caution', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
    const result = await resp.json()
    if (result.success) {
      alert('Mouvement enregistré !')
      router.push('/reservation/fiche/' + form.value.idreservation)
    } else {
      alert('Erreur : ' + (result.error || JSON.stringify(result)))
    }
  } catch (e) {
    alert('Erreur JS : ' + e)
  }
}
</script>

<style scoped>
.content-wrapper {
  padding: 2rem;
}
.row {
  margin-bottom: 1rem;
}
</style>