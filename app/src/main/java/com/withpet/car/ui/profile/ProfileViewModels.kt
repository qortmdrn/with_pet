package com.withpet.car.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.withpet.car.domain.model.PetProfile
import com.withpet.car.domain.model.Species
import com.withpet.car.domain.repository.PetProfileRepository
import com.withpet.car.domain.usecase.DrivingRestrictionMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class ProfileListUiState(
    val pets: List<PetProfile> = emptyList(),
    val representativeId: Long? = null,
    val restricted: Boolean = false,
)

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val repository: PetProfileRepository,
    restriction: DrivingRestrictionMonitor,
) : ViewModel() {

    val state: StateFlow<ProfileListUiState> = combine(
        repository.observeAll().onStart { emit(emptyList()) },
        repository.observeRepresentativeId().onStart { emit(null) },
        restriction.isRestricted,
    ) { pets, repId, restricted -> ProfileListUiState(pets, repId, restricted) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileListUiState())

    fun setRepresentative(id: Long) {
        viewModelScope.launch { repository.setRepresentative(id) }
    }
}

data class ProfileForm(
    val id: Long = 0,
    val name: String = "",
    val species: Species = Species.DOG,
    val breed: String = "",
    val birthYear: String = "",
    val weightKg: String = "",
    val photoUri: String? = null,
) {
    val nameError: Boolean get() = name.isBlank()
    val birthYearError: Boolean get() = birthYear.isNotBlank() && birthYear.toIntOrNull()?.let { it in 1980..2100 } != true
    val weightError: Boolean get() = weightKg.isNotBlank() && weightKg.toFloatOrNull()?.let { it > 0f && it < 200f } != true
    val isValid: Boolean get() = !nameError && !birthYearError && !weightError

    fun toDomain() = PetProfile(
        id = id,
        name = name.trim(),
        species = species,
        breed = breed.trim().ifBlank { null },
        birthYear = birthYear.toIntOrNull(),
        weightKg = weightKg.toFloatOrNull(),
        photoUri = photoUri,
    )

    companion object {
        fun from(p: PetProfile) = ProfileForm(
            id = p.id,
            name = p.name,
            species = p.species,
            breed = p.breed.orEmpty(),
            birthYear = p.birthYear?.toString().orEmpty(),
            weightKg = p.weightKg?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }.orEmpty(),
            photoUri = p.photoUri,
        )
    }
}

data class ProfileEditUiState(
    val form: ProfileForm = ProfileForm(),
    val isNew: Boolean = true,
    val showErrors: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PetProfileRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val petId: Long = savedStateHandle.get<Long>(ARG_PET_ID) ?: 0L
    private val _state = MutableStateFlow(ProfileEditUiState(isNew = petId == 0L))
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    init {
        if (petId != 0L) {
            viewModelScope.launch {
                repository.get(petId)?.let { pet -> _state.update { it.copy(form = ProfileForm.from(pet)) } }
            }
        }
    }

    fun edit(transform: (ProfileForm) -> ProfileForm) {
        _state.update { it.copy(form = transform(it.form)) }
    }

    /** 고른 사진을 앱 내부 저장소로 복사한다 (로컬 저장, 외부 전송 없음). */
    fun setPhoto(source: Uri) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
                    val target = File(dir, "${UUID.randomUUID()}.jpg")
                    context.contentResolver.openInputStream(source)!!.use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    }
                    Uri.fromFile(target).toString()
                }.getOrNull()
            }
            if (saved != null) {
                val old = _state.value.form.photoUri
                edit { it.copy(photoUri = saved) }
                if (old != null && old != saved) deletePhotoFile(old)
            }
        }
    }

    fun save() {
        val form = _state.value.form
        if (!form.isValid) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            repository.save(form.toDomain())
            _state.update { it.copy(done = true) }
        }
    }

    fun delete() {
        if (petId == 0L) return
        viewModelScope.launch {
            _state.value.form.photoUri?.let { deletePhotoFile(it) }
            repository.delete(petId)
            _state.update { it.copy(done = true) }
        }
    }

    private suspend fun deletePhotoFile(uri: String) = withContext(Dispatchers.IO) {
        Uri.parse(uri).path?.let { File(it).delete() }
    }

    companion object {
        const val ARG_PET_ID = "petId"
        private const val PHOTO_DIR = "pet_photos"
    }
}
