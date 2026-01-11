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
export async function validerProforma(id, userId = 'admin') {
	const res = await fetch(`/asynclocation/api/proforma/valider/${id}`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
			'user': userId
		}
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
