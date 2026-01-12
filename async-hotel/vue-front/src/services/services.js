// Liste des réservations
export async function fetchReservations() {
	const res = await fetch('/asynclocation/api/reservation/liste')
	if (!res.ok) throw new Error('Erreur API: ' + res.status)
	return await res.json()
}

// Fiche réservation
export async function fetchReservationFiche(id) {
  const res = await fetch(`/asynclocation/api/reservation/fiche/${id}`)
  if (!res.ok) throw new Error('Erreur API: ' + res.status)
  return await res.json()
}

// Liste des proformas
export async function fetchProformas() {
	const res = await fetch('/asynclocation/api/proforma/liste')
	if (!res.ok) throw new Error('Erreur API: ' + res.status)
	return await res.json()
}

// Fiche proforma
export async function fetchProformaFiche(id) {
	const res = await fetch(`/asynclocation/api/proforma/fiche/${id}`)
	if (!res.ok) throw new Error('Erreur API: ' + res.status)
	return await res.json()
}

// Valider proforma (créer BC)
export async function validerProforma(id) {
    const res = await fetch(`/asynclocation/api/proforma/valider/${id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    return await res.json()
}

// Liste des clients pour proforma
export async function fetchProformaClients() {
	const res = await fetch('/asynclocation/api/proforma/clients')
	if (!res.ok) throw new Error('Erreur API: ' + res.status)
	return await res.json()
}

// Liste des produits pour proforma
export async function fetchProformaProduits() {
	const res = await fetch('/asynclocation/api/proforma/produits')
	if (!res.ok) throw new Error('Erreur API: ' + res.status)
	return await res.json()
}

// Créer une proforma
export async function creerProforma(data) {
	const res = await fetch('/asynclocation/api/proforma/creer', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
		credentials: 'include',
		body: JSON.stringify(data)
	})
	return await res.json()
}

// ============= CAISSE =============

// Charger la liste des caisses
export async function fetchCaisses() {
  const resp = await fetch('/asynclocation/api/caisse/liste')
  if (!resp.ok) throw new Error('Erreur API caisses')
  return await resp.json()
}

// Charger les stats de caution
export async function fetchCautionStats(idcaution) {
  const resp = await fetch(`/asynclocation/api/caisse/caution-stats?idcaution=${idcaution}`)
  if (!resp.ok) throw new Error('Erreur API stats caution')
  return await resp.json()
}

// Charger le formulaire de mouvement caution
export async function fetchFormulaireMvtCaution({ idcaution, idreservation, type }) {
  const resp = await fetch(`/asynclocation/api/caisse/formulaire-mvt-caution?idcaution=${idcaution}&idreservation=${idreservation}&type=${type}`)
  if (!resp.ok) throw new Error('Erreur API formulaire caution')
  return await resp.json()
}

// Enregistrer un mouvement de caution
export async function postMvtCaisseCaution(payload) {
  const resp = await fetch('/asynclocation/api/caisse/mvt-caution', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  if (!resp.ok) throw new Error('Erreur lors de l\'enregistrement du mouvement de caution')
  return await resp.json()
}

// Enregistrer un mouvement de caisse location
export async function postMvtCaisseLocation(payload) {
  const resp = await fetch('/asynclocation/api/caisse/mvt-location', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  if (!resp.ok) throw new Error('Erreur lors de l\'enregistrement du mouvement de location')
  return await resp.json()
}

// Récupérer la liste des mouvements de caisse
export async function fetchMvtCaisseListe({ etat = '', designation = '', daty1 = '', daty2 = '' }) {
  const params = new URLSearchParams()
  if (etat) params.append('etat', etat)
  if (designation) params.append('designation', designation)
  if (daty1) params.append('daty1', daty1)
  if (daty2) params.append('daty2', daty2)
  
  const resp = await fetch(`/asynclocation/api/caisse/mvt-caisse-liste?${params.toString()}`)
  if (!resp.ok) throw new Error('Erreur API liste mouvements caisse')
  return await resp.json()
}

// ============= VENTE =============

// Récupérer la fiche d'une vente
export async function fetchVenteFiche(id) {
  const resp = await fetch(`/asynclocation/api/vente/fiche/${id}`)
  if (!resp.ok) throw new Error('Erreur API fiche vente')
  return await resp.json()
}

// livraisonnnnnnnnnnnnnnn

export async function getLivraisonFormulaire(idresa) {
    const res = await fetch(`/asynclocation/api/checkin/livraison-formulaire?idresa=${idresa}`)
    return await res.json()
}

// Valider la livraison
export async function validerLivraison(data) {
    const res = await fetch('/asynclocation/api/checkin/livraison-valider', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    return await res.json()
}


export async function getReceptionFormulaire(idresa) {
    const res = await fetch(`/asynclocation/api/checkout/reception-formulaire?idresa=${idresa}`)
    return await res.json()
}

// Valider la réception (checkout)
export async function validerReception(data) {
    const res = await fetch('/asynclocation/api/checkout/reception-valider', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    return await res.json()
}